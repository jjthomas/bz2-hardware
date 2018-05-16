#include <stdint.h>
#include <sys/time.h>
#include <fstream>
#include <stdlib.h>
#include <string.h>

using namespace std;

#define MIN(x, y) (((x) < (y)) ? (x) : (y))
#define MAX(x, y) (((x) > (y)) ? (x) : (y))

#define BATCH_SIZE 8
#define TREE_SIZE 4095

typedef uint8_t uint1_t;

typedef struct {
  uint8_t field;
  uint32_t split;
} node;

void run(uint32_t *input, uint32_t input_count, uint32_t *output, uint32_t *output_count) {
  uint32_t input_idx = 0;
  uint32_t output_idx = 0;
  uint8_t words_consumed = 0;
  uint32_t buffer[BATCH_SIZE];
  node tree[TREE_SIZE];

  for (int i = 0; i < TREE_SIZE; i++) {
    tree[i].field = rand() % BATCH_SIZE;
    tree[i].split = 1073741824; // midpoint of rand values
  }

  for (uint32_t i = input_idx; i < input_count; i++) {
    buffer[words_consumed++] = input[i];
    if (words_consumed == BATCH_SIZE) {
      int tree_idx = 0;
      while (tree_idx < TREE_SIZE / 2) {
        node cur = tree[tree_idx];
        tree_idx = buffer[cur.field] < cur.split ? tree_idx * 2 + 1 : tree_idx * 2 + 2;
      }
      output[output_idx++] = tree[tree_idx].split;
      words_consumed = 0;
    }
  }
  *output_count = output_idx;
}

int main(int argc, char **argv) {
  uint32_t CHARS = atoi(argv[1]);
  uint32_t NUM_THREADS = atoi(argv[2]);

  // extracts "ad_id" and "ad_type"
  uint8_t seq_confs[] = {1, 34, 2, 97, 3, 100, 4, 95, 5, 105, 6, 100, 200, 34, 8, 121, 9, 112, 10, 101, 200, 34};
  uint8_t split_confs[] = {4, 7, 116};

  ifstream infile("kafka-json.txt");
  string line;

  uint32_t input_buf_size = (sizeof(seq_confs) + sizeof(split_confs) + CHARS + 4 - 1) / 4 * 4;
  uint8_t *input_buf = new uint8_t[input_buf_size * NUM_THREADS];
  uint32_t chars = 0;
  memcpy(input_buf + chars, seq_confs, sizeof(seq_confs));
  chars += sizeof(seq_confs);
  memcpy(input_buf + chars, split_confs, sizeof(split_confs));
  chars += sizeof(split_confs);
  while (getline(infile, line)) {
    if (chars + line.length() > input_buf_size) {
      break;
    }
    memcpy(input_buf + chars, line.c_str(), line.length());
    chars += line.length();
  }
  chars = chars / 4 * 4;
  for (uint32_t i = 1; i < NUM_THREADS; i++) {
    memcpy(input_buf + i * chars, input_buf, chars);
  }
  for (uint32_t i = 0; i < chars / 4 * NUM_THREADS; i++) {
    ((uint32_t *)input_buf)[i] = rand();
  }
  uint8_t *output_buf = new uint8_t[chars * 2 * NUM_THREADS];
  uint32_t *output_count = new uint32_t[NUM_THREADS];

  struct timeval start, end, diff;
  gettimeofday(&start, 0);
  #pragma omp parallel for
  for (uint32_t i = 0; i < NUM_THREADS; i++) {
    run((uint32_t *)(input_buf + chars * i), chars / 4,
      (uint32_t *)(output_buf + chars * 2 * i), output_count + i);
  }
  gettimeofday(&end, 0);
  timersub(&end, &start, &diff);
  double secs = diff.tv_sec + diff.tv_usec / 1000000.0;
  printf("%.2f MB/s, %d input tokens, %d output tokens, random output byte: %d\n",
    (chars * NUM_THREADS) / 1000000.0 / secs, chars / 4,
    output_count[0], output_count[0] == 0 ? 0 : output_buf[rand() % (output_count[0] * 4)]);
  return 0;
}
