`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif

module StreamingMemoryController(
  input          clock,
  input          reset,
  output [63:0]  io_axi_inputMemAddrs_0,
  output         io_axi_inputMemAddrValids_0,
  output [7:0]   io_axi_inputMemAddrLens_0,
  input          io_axi_inputMemAddrReadys_0,
  input  [511:0] io_axi_inputMemBlocks_0,
  input          io_axi_inputMemBlockValids_0,
  output         io_axi_inputMemBlockReadys_0,
  output [63:0]  io_axi_outputMemAddrs_0,
  output         io_axi_outputMemAddrValids_0,
  output [7:0]   io_axi_outputMemAddrLens_0,
  output [15:0]  io_axi_outputMemAddrIds_0,
  input          io_axi_outputMemAddrReadys_0,
  output [511:0] io_axi_outputMemBlocks_0,
  output         io_axi_outputMemBlockValids_0,
  output         io_axi_outputMemBlockLasts_0,
  input          io_axi_outputMemBlockReadys_0,
  output         io_axi_finished,
  output [31:0]  io_streamingCores_0_metadataPtr,
  input  [31:0]  io_streamingCores_0_inputMemAddr,
  input          io_streamingCores_0_inputMemAddrValid,
  input          io_streamingCores_0_inputMemAddrsFinished,
  output [31:0]  io_streamingCores_0_inputMemBlock,
  output [4:0]   io_streamingCores_0_inputMemIdx,
  output         io_streamingCores_0_inputMemBlockValid,
  input  [31:0]  io_streamingCores_0_outputMemAddr,
  input          io_streamingCores_0_outputMemAddrValid,
  output         io_streamingCores_0_outputMemAddrReady,
  input  [31:0]  io_streamingCores_0_outputMemBlock,
  input  [4:0]   io_streamingCores_0_outputMemIdx,
  input          io_streamingCores_0_outputMemBlockValid,
  input          io_streamingCores_0_outputFinished,
  output [31:0]  io_streamingCores_1_metadataPtr,
  input  [31:0]  io_streamingCores_1_inputMemAddr,
  input          io_streamingCores_1_inputMemAddrValid,
  input          io_streamingCores_1_inputMemAddrsFinished,
  output [31:0]  io_streamingCores_1_inputMemBlock,
  output [4:0]   io_streamingCores_1_inputMemIdx,
  output         io_streamingCores_1_inputMemBlockValid,
  input  [31:0]  io_streamingCores_1_outputMemAddr,
  input          io_streamingCores_1_outputMemAddrValid,
  output         io_streamingCores_1_outputMemAddrReady,
  input  [31:0]  io_streamingCores_1_outputMemBlock,
  input  [4:0]   io_streamingCores_1_outputMemIdx,
  input          io_streamingCores_1_outputMemBlockValid,
  input          io_streamingCores_1_outputFinished
);
  reg  _T_250;
  reg [31:0] _RAND_0;
  reg  _T_253;
  reg [31:0] _RAND_1;
  reg  _T_256;
  reg [31:0] _RAND_2;
  reg [31:0] _T_258;
  reg [31:0] _RAND_3;
  reg  _T_260;
  reg [31:0] _RAND_4;
  reg  _T_262;
  reg [31:0] _RAND_5;
  wire [31:0] _T_266;
  wire  _T_270;
  wire  _T_274;
  wire [31:0] _T_277_0;
  wire  _T_283_0;
  wire  _T_289_0;
  reg [31:0] _T_294;
  reg [31:0] _RAND_6;
  reg  _T_296;
  reg [31:0] _RAND_7;
  reg  _T_298;
  reg [31:0] _RAND_8;
  reg [31:0] _T_300;
  reg [31:0] _RAND_9;
  reg [4:0] _T_302;
  reg [31:0] _RAND_10;
  reg  _T_305;
  reg [31:0] _RAND_11;
  wire [31:0] _T_309;
  wire  _T_313;
  wire  _T_317;
  wire [31:0] _T_321;
  wire [4:0] _T_325;
  wire  _T_326;
  wire [31:0] _T_329_0;
  wire  _T_335_0;
  wire  _T_341_0;
  wire [31:0] _T_347_0;
  wire [4:0] _T_353_0;
  wire  _T_359_0;
  reg  _T_365;
  reg [31:0] _RAND_12;
  reg  _T_379_0;
  reg [31:0] _RAND_13;
  reg  _T_400_0;
  reg [31:0] _RAND_14;
  reg [31:0] _T_481_0_0;
  reg [31:0] _RAND_15;
  reg [31:0] _T_481_0_1;
  reg [31:0] _RAND_16;
  reg [31:0] _T_481_0_2;
  reg [31:0] _RAND_17;
  reg [31:0] _T_481_0_3;
  reg [31:0] _RAND_18;
  reg [31:0] _T_481_0_4;
  reg [31:0] _RAND_19;
  reg [31:0] _T_481_0_5;
  reg [31:0] _RAND_20;
  reg [31:0] _T_481_0_6;
  reg [31:0] _RAND_21;
  reg [31:0] _T_481_0_7;
  reg [31:0] _RAND_22;
  reg [31:0] _T_481_0_8;
  reg [31:0] _RAND_23;
  reg [31:0] _T_481_0_9;
  reg [31:0] _RAND_24;
  reg [31:0] _T_481_0_10;
  reg [31:0] _RAND_25;
  reg [31:0] _T_481_0_11;
  reg [31:0] _RAND_26;
  reg [31:0] _T_481_0_12;
  reg [31:0] _RAND_27;
  reg [31:0] _T_481_0_13;
  reg [31:0] _RAND_28;
  reg [31:0] _T_481_0_14;
  reg [31:0] _RAND_29;
  reg [31:0] _T_481_0_15;
  reg [31:0] _RAND_30;
  reg [31:0] _T_481_0_16;
  reg [31:0] _RAND_31;
  reg [31:0] _T_481_0_17;
  reg [31:0] _RAND_32;
  reg [31:0] _T_481_0_18;
  reg [31:0] _RAND_33;
  reg [31:0] _T_481_0_19;
  reg [31:0] _RAND_34;
  reg [31:0] _T_481_0_20;
  reg [31:0] _RAND_35;
  reg [31:0] _T_481_0_21;
  reg [31:0] _RAND_36;
  reg [31:0] _T_481_0_22;
  reg [31:0] _RAND_37;
  reg [31:0] _T_481_0_23;
  reg [31:0] _RAND_38;
  reg [31:0] _T_481_0_24;
  reg [31:0] _RAND_39;
  reg [31:0] _T_481_0_25;
  reg [31:0] _RAND_40;
  reg [31:0] _T_481_0_26;
  reg [31:0] _RAND_41;
  reg [31:0] _T_481_0_27;
  reg [31:0] _RAND_42;
  reg [31:0] _T_481_0_28;
  reg [31:0] _RAND_43;
  reg [31:0] _T_481_0_29;
  reg [31:0] _RAND_44;
  reg [31:0] _T_481_0_30;
  reg [31:0] _RAND_45;
  reg [31:0] _T_481_0_31;
  reg [31:0] _RAND_46;
  reg [4:0] _T_700_0;
  reg [31:0] _RAND_47;
  reg  _T_721_0;
  reg [31:0] _RAND_48;
  reg  _T_731;
  reg [31:0] _RAND_49;
  reg  _T_734;
  reg [31:0] _RAND_50;
  reg  _T_737;
  reg [31:0] _RAND_51;
  wire  _T_743;
  wire [1:0] _T_745;
  wire  _T_746;
  wire  _GEN_3;
  wire  _T_753;
  wire  _T_759;
  wire  _T_760;
  wire  _T_762;
  wire  _T_767;
  wire  _T_774;
  wire  _T_775;
  wire  _GEN_4;
  wire  _T_787;
  wire [1:0] _T_792;
  wire  _T_793;
  wire  _T_794;
  wire [1:0] _T_798;
  wire  _T_799;
  wire  _GEN_5;
  wire  _GEN_6;
  wire  _GEN_7;
  wire  _GEN_8;
  wire  _GEN_9;
  wire  _GEN_10;
  wire  _GEN_11;
  wire  _GEN_12;
  wire  _T_801;
  wire [5:0] _T_807;
  wire [4:0] _T_808;
  wire  _GEN_13;
  wire  _GEN_14;
  wire  _GEN_15;
  wire [4:0] _GEN_16;
  wire  _GEN_17;
  wire  _GEN_18;
  wire  _GEN_19;
  wire [4:0] _GEN_20;
  wire  _T_809;
  wire [1:0] _T_814;
  wire  _T_815;
  wire  _T_816;
  wire  _T_818;
  wire [31:0] _T_922;
  wire [31:0] _GEN_21;
  wire [31:0] _T_1028;
  wire [31:0] _GEN_22;
  wire [31:0] _T_1134;
  wire [31:0] _GEN_23;
  wire [31:0] _T_1240;
  wire [31:0] _GEN_24;
  wire [31:0] _T_1346;
  wire [31:0] _GEN_25;
  wire [31:0] _T_1452;
  wire [31:0] _GEN_26;
  wire [31:0] _T_1558;
  wire [31:0] _GEN_27;
  wire [31:0] _T_1664;
  wire [31:0] _GEN_28;
  wire [31:0] _T_1770;
  wire [31:0] _GEN_29;
  wire [31:0] _T_1876;
  wire [31:0] _GEN_30;
  wire [31:0] _T_1982;
  wire [31:0] _GEN_31;
  wire [31:0] _T_2088;
  wire [31:0] _GEN_32;
  wire [31:0] _T_2194;
  wire [31:0] _GEN_33;
  wire [31:0] _T_2300;
  wire [31:0] _GEN_34;
  wire [31:0] _T_2406;
  wire [31:0] _GEN_35;
  wire [31:0] _T_2512;
  wire [31:0] _GEN_36;
  wire [31:0] _GEN_37;
  wire [31:0] _GEN_38;
  wire [31:0] _GEN_39;
  wire [31:0] _GEN_40;
  wire [31:0] _GEN_41;
  wire [31:0] _GEN_42;
  wire [31:0] _GEN_43;
  wire [31:0] _GEN_44;
  wire [31:0] _GEN_45;
  wire [31:0] _GEN_46;
  wire [31:0] _GEN_47;
  wire [31:0] _GEN_48;
  wire [31:0] _GEN_49;
  wire [31:0] _GEN_50;
  wire [31:0] _GEN_51;
  wire [31:0] _GEN_52;
  wire  _GEN_53;
  wire [31:0] _GEN_54;
  wire [31:0] _GEN_55;
  wire [31:0] _GEN_56;
  wire [31:0] _GEN_57;
  wire [31:0] _GEN_58;
  wire [31:0] _GEN_59;
  wire [31:0] _GEN_60;
  wire [31:0] _GEN_61;
  wire [31:0] _GEN_62;
  wire [31:0] _GEN_63;
  wire [31:0] _GEN_64;
  wire [31:0] _GEN_65;
  wire [31:0] _GEN_66;
  wire [31:0] _GEN_67;
  wire [31:0] _GEN_68;
  wire [31:0] _GEN_69;
  wire [31:0] _GEN_70;
  wire [31:0] _GEN_71;
  wire [31:0] _GEN_72;
  wire [31:0] _GEN_73;
  wire [31:0] _GEN_74;
  wire [31:0] _GEN_75;
  wire [31:0] _GEN_76;
  wire [31:0] _GEN_77;
  wire [31:0] _GEN_78;
  wire [31:0] _GEN_79;
  wire [31:0] _GEN_80;
  wire [31:0] _GEN_81;
  wire [31:0] _GEN_82;
  wire [31:0] _GEN_83;
  wire [31:0] _GEN_84;
  wire [31:0] _GEN_85;
  wire  _T_4209;
  wire  _T_4212;
  wire  _T_4222;
  wire  _T_4223;
  wire  _T_4229;
  wire  _T_4230;
  wire  _T_4231;
  wire  _GEN_86;
  wire  _GEN_87;
  wire  _T_4243;
  wire [1:0] _T_4248;
  wire  _T_4249;
  wire  _T_4250;
  wire [1:0] _T_4253;
  wire  _T_4254;
  wire  _GEN_88;
  wire  _GEN_89;
  wire  _GEN_90;
  wire  _GEN_91;
  wire  _GEN_92;
  wire  _GEN_93;
  wire  _T_4265;
  wire [31:0] _GEN_0;
  wire [31:0] _GEN_94;
  wire [31:0] _GEN_95;
  wire [31:0] _GEN_96;
  wire [31:0] _GEN_97;
  wire [31:0] _GEN_98;
  wire [31:0] _GEN_99;
  wire [31:0] _GEN_100;
  wire [31:0] _GEN_101;
  wire [31:0] _GEN_102;
  wire [31:0] _GEN_103;
  wire [31:0] _GEN_104;
  wire [31:0] _GEN_105;
  wire [31:0] _GEN_106;
  wire [31:0] _GEN_107;
  wire [31:0] _GEN_108;
  wire [31:0] _GEN_109;
  wire [31:0] _GEN_110;
  wire [31:0] _GEN_111;
  wire [31:0] _GEN_112;
  wire [31:0] _GEN_113;
  wire [31:0] _GEN_114;
  wire [31:0] _GEN_115;
  wire [31:0] _GEN_116;
  wire [31:0] _GEN_117;
  wire [31:0] _GEN_118;
  wire [31:0] _GEN_119;
  wire [31:0] _GEN_120;
  wire [31:0] _GEN_121;
  wire [31:0] _GEN_122;
  wire [31:0] _GEN_123;
  wire [31:0] _GEN_124;
  wire  _T_4269;
  wire [1:0] _T_4272;
  wire [1:0] _T_4273;
  wire  _T_4274;
  wire  _T_4275;
  wire  _T_4277;
  wire  _T_4278;
  wire  _T_4280;
  wire  _T_4282;
  wire [31:0] _GEN_1;
  wire  _T_4299;
  reg  _T_4302;
  reg [31:0] _RAND_52;
  wire  _T_4304;
  wire [1:0] _T_4306;
  wire  _T_4307;
  wire  _GEN_125;
  reg  _T_4321_0;
  reg [31:0] _RAND_53;
  wire  _T_4330;
  reg [31:0] _T_4334_0;
  reg [31:0] _RAND_54;
  reg [31:0] _T_4411_0_0;
  reg [31:0] _RAND_55;
  reg [31:0] _T_4411_0_1;
  reg [31:0] _RAND_56;
  reg [31:0] _T_4411_0_2;
  reg [31:0] _RAND_57;
  reg [31:0] _T_4411_0_3;
  reg [31:0] _RAND_58;
  reg [31:0] _T_4411_0_4;
  reg [31:0] _RAND_59;
  reg [31:0] _T_4411_0_5;
  reg [31:0] _RAND_60;
  reg [31:0] _T_4411_0_6;
  reg [31:0] _RAND_61;
  reg [31:0] _T_4411_0_7;
  reg [31:0] _RAND_62;
  reg [31:0] _T_4411_0_8;
  reg [31:0] _RAND_63;
  reg [31:0] _T_4411_0_9;
  reg [31:0] _RAND_64;
  reg [31:0] _T_4411_0_10;
  reg [31:0] _RAND_65;
  reg [31:0] _T_4411_0_11;
  reg [31:0] _RAND_66;
  reg [31:0] _T_4411_0_12;
  reg [31:0] _RAND_67;
  reg [31:0] _T_4411_0_13;
  reg [31:0] _RAND_68;
  reg [31:0] _T_4411_0_14;
  reg [31:0] _RAND_69;
  reg [31:0] _T_4411_0_15;
  reg [31:0] _RAND_70;
  reg [31:0] _T_4411_0_16;
  reg [31:0] _RAND_71;
  reg [31:0] _T_4411_0_17;
  reg [31:0] _RAND_72;
  reg [31:0] _T_4411_0_18;
  reg [31:0] _RAND_73;
  reg [31:0] _T_4411_0_19;
  reg [31:0] _RAND_74;
  reg [31:0] _T_4411_0_20;
  reg [31:0] _RAND_75;
  reg [31:0] _T_4411_0_21;
  reg [31:0] _RAND_76;
  reg [31:0] _T_4411_0_22;
  reg [31:0] _RAND_77;
  reg [31:0] _T_4411_0_23;
  reg [31:0] _RAND_78;
  reg [31:0] _T_4411_0_24;
  reg [31:0] _RAND_79;
  reg [31:0] _T_4411_0_25;
  reg [31:0] _RAND_80;
  reg [31:0] _T_4411_0_26;
  reg [31:0] _RAND_81;
  reg [31:0] _T_4411_0_27;
  reg [31:0] _RAND_82;
  reg [31:0] _T_4411_0_28;
  reg [31:0] _RAND_83;
  reg [31:0] _T_4411_0_29;
  reg [31:0] _RAND_84;
  reg [31:0] _T_4411_0_30;
  reg [31:0] _RAND_85;
  reg [31:0] _T_4411_0_31;
  reg [31:0] _RAND_86;
  reg  _T_4630_0;
  reg [31:0] _RAND_87;
  reg  _T_4640;
  reg [31:0] _RAND_88;
  reg  _T_4643;
  reg [31:0] _RAND_89;
  reg  _T_4646;
  reg [31:0] _RAND_90;
  reg  _T_4649;
  reg [31:0] _RAND_91;
  reg  _T_4652;
  reg [31:0] _RAND_92;
  wire  _T_4656;
  wire  _T_4657;
  wire  _GEN_126;
  wire  _T_4661;
  wire  _T_4663;
  wire  _T_4664;
  wire  _GEN_127;
  wire [31:0] _GEN_2;
  wire [31:0] _GEN_128;
  wire [31:0] _GEN_129;
  wire [31:0] _GEN_130;
  wire [31:0] _GEN_131;
  wire [31:0] _GEN_132;
  wire [31:0] _GEN_133;
  wire [31:0] _GEN_134;
  wire [31:0] _GEN_135;
  wire [31:0] _GEN_136;
  wire [31:0] _GEN_137;
  wire [31:0] _GEN_138;
  wire [31:0] _GEN_139;
  wire [31:0] _GEN_140;
  wire [31:0] _GEN_141;
  wire [31:0] _GEN_142;
  wire [31:0] _GEN_143;
  wire [31:0] _GEN_144;
  wire [31:0] _GEN_145;
  wire [31:0] _GEN_146;
  wire [31:0] _GEN_147;
  wire [31:0] _GEN_148;
  wire [31:0] _GEN_149;
  wire [31:0] _GEN_150;
  wire [31:0] _GEN_151;
  wire [31:0] _GEN_152;
  wire [31:0] _GEN_153;
  wire [31:0] _GEN_154;
  wire [31:0] _GEN_155;
  wire [31:0] _GEN_156;
  wire [31:0] _GEN_157;
  wire [31:0] _GEN_158;
  wire [31:0] _GEN_159;
  wire [31:0] _GEN_160;
  wire [31:0] _GEN_161;
  wire [31:0] _GEN_162;
  wire [31:0] _GEN_163;
  wire [31:0] _GEN_164;
  wire [31:0] _GEN_165;
  wire [31:0] _GEN_166;
  wire [31:0] _GEN_167;
  wire [31:0] _GEN_168;
  wire [31:0] _GEN_169;
  wire [31:0] _GEN_170;
  wire [31:0] _GEN_171;
  wire [31:0] _GEN_172;
  wire [31:0] _GEN_173;
  wire [31:0] _GEN_174;
  wire [31:0] _GEN_175;
  wire [31:0] _GEN_176;
  wire [31:0] _GEN_177;
  wire [31:0] _GEN_178;
  wire [31:0] _GEN_179;
  wire [31:0] _GEN_180;
  wire [31:0] _GEN_181;
  wire [31:0] _GEN_182;
  wire [31:0] _GEN_183;
  wire [31:0] _GEN_184;
  wire [31:0] _GEN_185;
  wire [31:0] _GEN_186;
  wire [31:0] _GEN_187;
  wire [31:0] _GEN_188;
  wire [31:0] _GEN_189;
  wire [31:0] _GEN_190;
  wire [31:0] _GEN_191;
  wire  _T_4670;
  wire  _T_4673;
  wire  _T_4679;
  wire  _GEN_192;
  wire [31:0] _GEN_193;
  wire  _T_4688;
  wire  _T_4689;
  wire [63:0] _T_4794;
  wire [63:0] _T_4795;
  wire [127:0] _T_4796;
  wire [63:0] _T_4797;
  wire [63:0] _T_4798;
  wire [127:0] _T_4799;
  wire [255:0] _T_4800;
  wire [63:0] _T_4801;
  wire [63:0] _T_4802;
  wire [127:0] _T_4803;
  wire [63:0] _T_4804;
  wire [63:0] _T_4805;
  wire [127:0] _T_4806;
  wire [255:0] _T_4807;
  wire [511:0] _T_4808;
  wire [63:0] _T_4809;
  wire [63:0] _T_4810;
  wire [127:0] _T_4811;
  wire [63:0] _T_4812;
  wire [63:0] _T_4813;
  wire [127:0] _T_4814;
  wire [255:0] _T_4815;
  wire [63:0] _T_4816;
  wire [63:0] _T_4817;
  wire [127:0] _T_4818;
  wire [63:0] _T_4819;
  wire [63:0] _T_4820;
  wire [127:0] _T_4821;
  wire [255:0] _T_4822;
  wire [511:0] _T_4823;
  wire [1023:0] _T_4824;
  wire [511:0] _T_4825;
  wire [511:0] _T_4828;
  wire [511:0] _T_4829;
  wire  _T_4836;
  wire  _T_4843;
  wire  _T_4844;
  wire  _T_4846;
  wire [1:0] _T_4849;
  wire  _T_4850;
  wire  _GEN_194;
  wire  _GEN_195;
  wire  _GEN_196;
  wire  _GEN_197;
  wire  _T_4851;
  wire [1:0] _T_4856;
  wire  _T_4857;
  wire  _T_4858;
  wire  _GEN_198;
  wire  _T_4862;
  wire  _T_4869;
  wire  _T_4870;
  wire  _T_4871;
  wire  _T_4872;
  wire  _T_4874;
  wire [1:0] _T_4879;
  wire  _T_4880;
  wire  _T_4881;
  wire [1:0] _T_4890;
  wire  _T_4891;
  wire  _GEN_199;
  wire  _GEN_200;
  wire  _GEN_201;
  wire  _GEN_202;
  wire  _GEN_203;
  wire  _GEN_204;
  wire  _GEN_205;
  wire  _GEN_206;
  wire  _GEN_207;
  wire  _GEN_208;
  wire  _GEN_209;
  wire  _GEN_210;
  wire  _GEN_211;
  wire  _GEN_212;
  wire  _GEN_213;
  wire  _GEN_214;
  wire  _T_4893;
  wire  _T_4907;
  wire  _T_4923;
  wire  cumFinished;
  assign _T_266 = _T_250 ? io_streamingCores_1_inputMemAddr : io_streamingCores_0_inputMemAddr;
  assign _T_270 = _T_250 ? io_streamingCores_1_inputMemAddrValid : io_streamingCores_0_inputMemAddrValid;
  assign _T_274 = _T_250 ? io_streamingCores_1_inputMemAddrsFinished : io_streamingCores_0_inputMemAddrsFinished;
  assign _T_309 = _T_256 ? io_streamingCores_1_outputMemAddr : io_streamingCores_0_outputMemAddr;
  assign _T_313 = _T_256 ? io_streamingCores_1_outputMemAddrValid : io_streamingCores_0_outputMemAddrValid;
  assign _T_317 = _T_256 ? io_streamingCores_1_outputMemBlockValid : io_streamingCores_0_outputMemBlockValid;
  assign _T_321 = _T_256 ? io_streamingCores_1_outputMemBlock : io_streamingCores_0_outputMemBlock;
  assign _T_325 = _T_256 ? io_streamingCores_1_outputMemIdx : io_streamingCores_0_outputMemIdx;
  assign _T_326 = io_streamingCores_1_outputFinished & io_streamingCores_0_outputFinished;
  assign _T_743 = _T_365 == 1'h0;
  assign _T_745 = _T_365 + 1'h1;
  assign _T_746 = _T_745[0:0];
  assign _GEN_3 = _T_743 ? _T_746 : _T_365;
  assign _T_753 = _T_365 & _T_283_0;
  assign _T_759 = _T_379_0 == 1'h0;
  assign _T_760 = _T_753 & _T_759;
  assign _T_762 = io_axi_inputMemAddrValids_0 & io_axi_inputMemAddrReadys_0;
  assign _T_767 = _T_365 & _T_289_0;
  assign _T_774 = _T_767 & _T_759;
  assign _T_775 = _T_762 | _T_774;
  assign _GEN_4 = io_axi_inputMemAddrValids_0 ? 1'h1 : _T_400_0;
  assign _T_787 = _T_731 == 1'h0;
  assign _T_792 = _T_250 + 1'h1;
  assign _T_793 = _T_792[0:0];
  assign _T_794 = _T_250 ? 1'h0 : _T_793;
  assign _T_798 = _T_731 + 1'h1;
  assign _T_799 = _T_798[0:0];
  assign _GEN_5 = _T_787 ? _T_794 : _T_250;
  assign _GEN_6 = _T_787 ? 1'h0 : _GEN_3;
  assign _GEN_7 = _T_787 ? 1'h0 : _T_799;
  assign _GEN_8 = _T_775 ? 1'h1 : _T_379_0;
  assign _GEN_9 = _T_775 ? _GEN_4 : _T_400_0;
  assign _GEN_10 = _T_775 ? _GEN_5 : _T_250;
  assign _GEN_11 = _T_775 ? _GEN_6 : _GEN_3;
  assign _GEN_12 = _T_775 ? _GEN_7 : _T_731;
  assign _T_801 = _T_700_0 == 5'h1f;
  assign _T_807 = _T_700_0 + 5'h1;
  assign _T_808 = _T_807[4:0];
  assign _GEN_13 = _T_801 ? 1'h0 : _GEN_8;
  assign _GEN_14 = _T_801 ? 1'h0 : _GEN_9;
  assign _GEN_15 = _T_801 ? 1'h0 : _T_721_0;
  assign _GEN_16 = _T_801 ? 5'h0 : _T_808;
  assign _GEN_17 = _T_721_0 ? _GEN_13 : _GEN_8;
  assign _GEN_18 = _T_721_0 ? _GEN_14 : _GEN_9;
  assign _GEN_19 = _T_721_0 ? _GEN_15 : _T_721_0;
  assign _GEN_20 = _T_721_0 ? _GEN_16 : _T_700_0;
  assign _T_809 = io_axi_inputMemBlockReadys_0 & io_axi_inputMemBlockValids_0;
  assign _T_814 = _T_737 + 1'h1;
  assign _T_815 = _T_814[0:0];
  assign _T_816 = _T_737 ? 1'h0 : _T_815;
  assign _T_818 = 1'h0 == _T_737;
  assign _T_922 = io_axi_inputMemBlocks_0[31:0];
  assign _GEN_21 = _T_818 ? _T_922 : _T_481_0_0;
  assign _T_1028 = io_axi_inputMemBlocks_0[63:32];
  assign _GEN_22 = _T_818 ? _T_1028 : _T_481_0_1;
  assign _T_1134 = io_axi_inputMemBlocks_0[95:64];
  assign _GEN_23 = _T_818 ? _T_1134 : _T_481_0_2;
  assign _T_1240 = io_axi_inputMemBlocks_0[127:96];
  assign _GEN_24 = _T_818 ? _T_1240 : _T_481_0_3;
  assign _T_1346 = io_axi_inputMemBlocks_0[159:128];
  assign _GEN_25 = _T_818 ? _T_1346 : _T_481_0_4;
  assign _T_1452 = io_axi_inputMemBlocks_0[191:160];
  assign _GEN_26 = _T_818 ? _T_1452 : _T_481_0_5;
  assign _T_1558 = io_axi_inputMemBlocks_0[223:192];
  assign _GEN_27 = _T_818 ? _T_1558 : _T_481_0_6;
  assign _T_1664 = io_axi_inputMemBlocks_0[255:224];
  assign _GEN_28 = _T_818 ? _T_1664 : _T_481_0_7;
  assign _T_1770 = io_axi_inputMemBlocks_0[287:256];
  assign _GEN_29 = _T_818 ? _T_1770 : _T_481_0_8;
  assign _T_1876 = io_axi_inputMemBlocks_0[319:288];
  assign _GEN_30 = _T_818 ? _T_1876 : _T_481_0_9;
  assign _T_1982 = io_axi_inputMemBlocks_0[351:320];
  assign _GEN_31 = _T_818 ? _T_1982 : _T_481_0_10;
  assign _T_2088 = io_axi_inputMemBlocks_0[383:352];
  assign _GEN_32 = _T_818 ? _T_2088 : _T_481_0_11;
  assign _T_2194 = io_axi_inputMemBlocks_0[415:384];
  assign _GEN_33 = _T_818 ? _T_2194 : _T_481_0_12;
  assign _T_2300 = io_axi_inputMemBlocks_0[447:416];
  assign _GEN_34 = _T_818 ? _T_2300 : _T_481_0_13;
  assign _T_2406 = io_axi_inputMemBlocks_0[479:448];
  assign _GEN_35 = _T_818 ? _T_2406 : _T_481_0_14;
  assign _T_2512 = io_axi_inputMemBlocks_0[511:480];
  assign _GEN_36 = _T_818 ? _T_2512 : _T_481_0_15;
  assign _GEN_37 = _T_737 ? _T_922 : _T_481_0_16;
  assign _GEN_38 = _T_737 ? _T_1028 : _T_481_0_17;
  assign _GEN_39 = _T_737 ? _T_1134 : _T_481_0_18;
  assign _GEN_40 = _T_737 ? _T_1240 : _T_481_0_19;
  assign _GEN_41 = _T_737 ? _T_1346 : _T_481_0_20;
  assign _GEN_42 = _T_737 ? _T_1452 : _T_481_0_21;
  assign _GEN_43 = _T_737 ? _T_1558 : _T_481_0_22;
  assign _GEN_44 = _T_737 ? _T_1664 : _T_481_0_23;
  assign _GEN_45 = _T_737 ? _T_1770 : _T_481_0_24;
  assign _GEN_46 = _T_737 ? _T_1876 : _T_481_0_25;
  assign _GEN_47 = _T_737 ? _T_1982 : _T_481_0_26;
  assign _GEN_48 = _T_737 ? _T_2088 : _T_481_0_27;
  assign _GEN_49 = _T_737 ? _T_2194 : _T_481_0_28;
  assign _GEN_50 = _T_737 ? _T_2300 : _T_481_0_29;
  assign _GEN_51 = _T_737 ? _T_2406 : _T_481_0_30;
  assign _GEN_52 = _T_737 ? _T_2512 : _T_481_0_31;
  assign _GEN_53 = _T_809 ? _T_816 : _T_737;
  assign _GEN_54 = _T_809 ? _GEN_21 : _T_481_0_0;
  assign _GEN_55 = _T_809 ? _GEN_22 : _T_481_0_1;
  assign _GEN_56 = _T_809 ? _GEN_23 : _T_481_0_2;
  assign _GEN_57 = _T_809 ? _GEN_24 : _T_481_0_3;
  assign _GEN_58 = _T_809 ? _GEN_25 : _T_481_0_4;
  assign _GEN_59 = _T_809 ? _GEN_26 : _T_481_0_5;
  assign _GEN_60 = _T_809 ? _GEN_27 : _T_481_0_6;
  assign _GEN_61 = _T_809 ? _GEN_28 : _T_481_0_7;
  assign _GEN_62 = _T_809 ? _GEN_29 : _T_481_0_8;
  assign _GEN_63 = _T_809 ? _GEN_30 : _T_481_0_9;
  assign _GEN_64 = _T_809 ? _GEN_31 : _T_481_0_10;
  assign _GEN_65 = _T_809 ? _GEN_32 : _T_481_0_11;
  assign _GEN_66 = _T_809 ? _GEN_33 : _T_481_0_12;
  assign _GEN_67 = _T_809 ? _GEN_34 : _T_481_0_13;
  assign _GEN_68 = _T_809 ? _GEN_35 : _T_481_0_14;
  assign _GEN_69 = _T_809 ? _GEN_36 : _T_481_0_15;
  assign _GEN_70 = _T_809 ? _GEN_37 : _T_481_0_16;
  assign _GEN_71 = _T_809 ? _GEN_38 : _T_481_0_17;
  assign _GEN_72 = _T_809 ? _GEN_39 : _T_481_0_18;
  assign _GEN_73 = _T_809 ? _GEN_40 : _T_481_0_19;
  assign _GEN_74 = _T_809 ? _GEN_41 : _T_481_0_20;
  assign _GEN_75 = _T_809 ? _GEN_42 : _T_481_0_21;
  assign _GEN_76 = _T_809 ? _GEN_43 : _T_481_0_22;
  assign _GEN_77 = _T_809 ? _GEN_44 : _T_481_0_23;
  assign _GEN_78 = _T_809 ? _GEN_45 : _T_481_0_24;
  assign _GEN_79 = _T_809 ? _GEN_46 : _T_481_0_25;
  assign _GEN_80 = _T_809 ? _GEN_47 : _T_481_0_26;
  assign _GEN_81 = _T_809 ? _GEN_48 : _T_481_0_27;
  assign _GEN_82 = _T_809 ? _GEN_49 : _T_481_0_28;
  assign _GEN_83 = _T_809 ? _GEN_50 : _T_481_0_29;
  assign _GEN_84 = _T_809 ? _GEN_51 : _T_481_0_30;
  assign _GEN_85 = _T_809 ? _GEN_52 : _T_481_0_31;
  assign _T_4209 = io_axi_inputMemBlockValids_0 & io_axi_inputMemBlockReadys_0;
  assign _T_4212 = _T_4209 & _T_737;
  assign _T_4222 = _T_400_0 == 1'h0;
  assign _T_4223 = _T_379_0 & _T_4222;
  assign _T_4229 = _T_721_0 == 1'h0;
  assign _T_4230 = _T_4223 & _T_4229;
  assign _T_4231 = _T_4212 | _T_4230;
  assign _GEN_86 = io_axi_inputMemBlockReadys_0 ? 1'h1 : _GEN_19;
  assign _GEN_87 = io_axi_inputMemBlockReadys_0 ? _GEN_17 : 1'h0;
  assign _T_4243 = _T_734 == 1'h0;
  assign _T_4248 = _T_253 + 1'h1;
  assign _T_4249 = _T_4248[0:0];
  assign _T_4250 = _T_253 ? 1'h0 : _T_4249;
  assign _T_4253 = _T_734 + 1'h1;
  assign _T_4254 = _T_4253[0:0];
  assign _GEN_88 = _T_4243 ? _T_4250 : _T_253;
  assign _GEN_89 = _T_4243 ? 1'h0 : _T_4254;
  assign _GEN_90 = _T_4231 ? _GEN_86 : _GEN_19;
  assign _GEN_91 = _T_4231 ? _GEN_87 : _GEN_17;
  assign _GEN_92 = _T_4231 ? _GEN_88 : _T_253;
  assign _GEN_93 = _T_4231 ? _GEN_89 : _T_734;
  assign _T_4265 = _T_400_0 & _T_4229;
  assign _GEN_94 = 5'h1 == _T_700_0 ? _T_481_0_1 : _T_481_0_0;
  assign _GEN_95 = 5'h2 == _T_700_0 ? _T_481_0_2 : _GEN_94;
  assign _GEN_96 = 5'h3 == _T_700_0 ? _T_481_0_3 : _GEN_95;
  assign _GEN_97 = 5'h4 == _T_700_0 ? _T_481_0_4 : _GEN_96;
  assign _GEN_98 = 5'h5 == _T_700_0 ? _T_481_0_5 : _GEN_97;
  assign _GEN_99 = 5'h6 == _T_700_0 ? _T_481_0_6 : _GEN_98;
  assign _GEN_100 = 5'h7 == _T_700_0 ? _T_481_0_7 : _GEN_99;
  assign _GEN_101 = 5'h8 == _T_700_0 ? _T_481_0_8 : _GEN_100;
  assign _GEN_102 = 5'h9 == _T_700_0 ? _T_481_0_9 : _GEN_101;
  assign _GEN_103 = 5'ha == _T_700_0 ? _T_481_0_10 : _GEN_102;
  assign _GEN_104 = 5'hb == _T_700_0 ? _T_481_0_11 : _GEN_103;
  assign _GEN_105 = 5'hc == _T_700_0 ? _T_481_0_12 : _GEN_104;
  assign _GEN_106 = 5'hd == _T_700_0 ? _T_481_0_13 : _GEN_105;
  assign _GEN_107 = 5'he == _T_700_0 ? _T_481_0_14 : _GEN_106;
  assign _GEN_108 = 5'hf == _T_700_0 ? _T_481_0_15 : _GEN_107;
  assign _GEN_109 = 5'h10 == _T_700_0 ? _T_481_0_16 : _GEN_108;
  assign _GEN_110 = 5'h11 == _T_700_0 ? _T_481_0_17 : _GEN_109;
  assign _GEN_111 = 5'h12 == _T_700_0 ? _T_481_0_18 : _GEN_110;
  assign _GEN_112 = 5'h13 == _T_700_0 ? _T_481_0_19 : _GEN_111;
  assign _GEN_113 = 5'h14 == _T_700_0 ? _T_481_0_20 : _GEN_112;
  assign _GEN_114 = 5'h15 == _T_700_0 ? _T_481_0_21 : _GEN_113;
  assign _GEN_115 = 5'h16 == _T_700_0 ? _T_481_0_22 : _GEN_114;
  assign _GEN_116 = 5'h17 == _T_700_0 ? _T_481_0_23 : _GEN_115;
  assign _GEN_117 = 5'h18 == _T_700_0 ? _T_481_0_24 : _GEN_116;
  assign _GEN_118 = 5'h19 == _T_700_0 ? _T_481_0_25 : _GEN_117;
  assign _GEN_119 = 5'h1a == _T_700_0 ? _T_481_0_26 : _GEN_118;
  assign _GEN_120 = 5'h1b == _T_700_0 ? _T_481_0_27 : _GEN_119;
  assign _GEN_121 = 5'h1c == _T_700_0 ? _T_481_0_28 : _GEN_120;
  assign _GEN_122 = 5'h1d == _T_700_0 ? _T_481_0_29 : _GEN_121;
  assign _GEN_123 = 5'h1e == _T_700_0 ? _T_481_0_30 : _GEN_122;
  assign _GEN_124 = 5'h1f == _T_700_0 ? _T_481_0_31 : _GEN_123;
  assign _T_4269 = _T_253 == 1'h0;
  assign _T_4272 = _T_253 - 1'h1;
  assign _T_4273 = $unsigned(_T_4272);
  assign _T_4274 = _T_4273[0:0];
  assign _T_4275 = _T_4269 ? 1'h1 : _T_4274;
  assign _T_4277 = 1'h0 < _T_734;
  assign _T_4278 = _T_4277 ? _T_253 : _T_4275;
  assign _T_4280 = _T_4278 == 1'h0;
  assign _T_4282 = _T_4280 ? _T_721_0 : 1'h0;
  assign _T_4299 = _T_4278 ? _T_721_0 : 1'h0;
  assign _T_4304 = _T_4302 != 1'h1;
  assign _T_4306 = _T_4302 + 1'h1;
  assign _T_4307 = _T_4306[0:0];
  assign _GEN_125 = _T_4304 ? _T_4307 : _T_4302;
  assign _T_4330 = _T_4321_0 == 1'h0;
  assign _T_4656 = _T_4652 == 1'h0;
  assign _T_4657 = _T_4302 & _T_4656;
  assign _GEN_126 = _T_4657 ? 1'h1 : _T_4652;
  assign _T_4661 = _T_4302 & _T_341_0;
  assign _T_4663 = _T_353_0 == 5'h1f;
  assign _T_4664 = _T_4661 & _T_4663;
  assign _GEN_127 = _T_4664 ? 1'h1 : _T_4630_0;
  assign _GEN_128 = 5'h0 == _T_353_0 ? _GEN_2 : _T_4411_0_0;
  assign _GEN_129 = 5'h1 == _T_353_0 ? _GEN_2 : _T_4411_0_1;
  assign _GEN_130 = 5'h2 == _T_353_0 ? _GEN_2 : _T_4411_0_2;
  assign _GEN_131 = 5'h3 == _T_353_0 ? _GEN_2 : _T_4411_0_3;
  assign _GEN_132 = 5'h4 == _T_353_0 ? _GEN_2 : _T_4411_0_4;
  assign _GEN_133 = 5'h5 == _T_353_0 ? _GEN_2 : _T_4411_0_5;
  assign _GEN_134 = 5'h6 == _T_353_0 ? _GEN_2 : _T_4411_0_6;
  assign _GEN_135 = 5'h7 == _T_353_0 ? _GEN_2 : _T_4411_0_7;
  assign _GEN_136 = 5'h8 == _T_353_0 ? _GEN_2 : _T_4411_0_8;
  assign _GEN_137 = 5'h9 == _T_353_0 ? _GEN_2 : _T_4411_0_9;
  assign _GEN_138 = 5'ha == _T_353_0 ? _GEN_2 : _T_4411_0_10;
  assign _GEN_139 = 5'hb == _T_353_0 ? _GEN_2 : _T_4411_0_11;
  assign _GEN_140 = 5'hc == _T_353_0 ? _GEN_2 : _T_4411_0_12;
  assign _GEN_141 = 5'hd == _T_353_0 ? _GEN_2 : _T_4411_0_13;
  assign _GEN_142 = 5'he == _T_353_0 ? _GEN_2 : _T_4411_0_14;
  assign _GEN_143 = 5'hf == _T_353_0 ? _GEN_2 : _T_4411_0_15;
  assign _GEN_144 = 5'h10 == _T_353_0 ? _GEN_2 : _T_4411_0_16;
  assign _GEN_145 = 5'h11 == _T_353_0 ? _GEN_2 : _T_4411_0_17;
  assign _GEN_146 = 5'h12 == _T_353_0 ? _GEN_2 : _T_4411_0_18;
  assign _GEN_147 = 5'h13 == _T_353_0 ? _GEN_2 : _T_4411_0_19;
  assign _GEN_148 = 5'h14 == _T_353_0 ? _GEN_2 : _T_4411_0_20;
  assign _GEN_149 = 5'h15 == _T_353_0 ? _GEN_2 : _T_4411_0_21;
  assign _GEN_150 = 5'h16 == _T_353_0 ? _GEN_2 : _T_4411_0_22;
  assign _GEN_151 = 5'h17 == _T_353_0 ? _GEN_2 : _T_4411_0_23;
  assign _GEN_152 = 5'h18 == _T_353_0 ? _GEN_2 : _T_4411_0_24;
  assign _GEN_153 = 5'h19 == _T_353_0 ? _GEN_2 : _T_4411_0_25;
  assign _GEN_154 = 5'h1a == _T_353_0 ? _GEN_2 : _T_4411_0_26;
  assign _GEN_155 = 5'h1b == _T_353_0 ? _GEN_2 : _T_4411_0_27;
  assign _GEN_156 = 5'h1c == _T_353_0 ? _GEN_2 : _T_4411_0_28;
  assign _GEN_157 = 5'h1d == _T_353_0 ? _GEN_2 : _T_4411_0_29;
  assign _GEN_158 = 5'h1e == _T_353_0 ? _GEN_2 : _T_4411_0_30;
  assign _GEN_159 = 5'h1f == _T_353_0 ? _GEN_2 : _T_4411_0_31;
  assign _GEN_160 = _T_341_0 ? _GEN_128 : _T_4411_0_0;
  assign _GEN_161 = _T_341_0 ? _GEN_129 : _T_4411_0_1;
  assign _GEN_162 = _T_341_0 ? _GEN_130 : _T_4411_0_2;
  assign _GEN_163 = _T_341_0 ? _GEN_131 : _T_4411_0_3;
  assign _GEN_164 = _T_341_0 ? _GEN_132 : _T_4411_0_4;
  assign _GEN_165 = _T_341_0 ? _GEN_133 : _T_4411_0_5;
  assign _GEN_166 = _T_341_0 ? _GEN_134 : _T_4411_0_6;
  assign _GEN_167 = _T_341_0 ? _GEN_135 : _T_4411_0_7;
  assign _GEN_168 = _T_341_0 ? _GEN_136 : _T_4411_0_8;
  assign _GEN_169 = _T_341_0 ? _GEN_137 : _T_4411_0_9;
  assign _GEN_170 = _T_341_0 ? _GEN_138 : _T_4411_0_10;
  assign _GEN_171 = _T_341_0 ? _GEN_139 : _T_4411_0_11;
  assign _GEN_172 = _T_341_0 ? _GEN_140 : _T_4411_0_12;
  assign _GEN_173 = _T_341_0 ? _GEN_141 : _T_4411_0_13;
  assign _GEN_174 = _T_341_0 ? _GEN_142 : _T_4411_0_14;
  assign _GEN_175 = _T_341_0 ? _GEN_143 : _T_4411_0_15;
  assign _GEN_176 = _T_341_0 ? _GEN_144 : _T_4411_0_16;
  assign _GEN_177 = _T_341_0 ? _GEN_145 : _T_4411_0_17;
  assign _GEN_178 = _T_341_0 ? _GEN_146 : _T_4411_0_18;
  assign _GEN_179 = _T_341_0 ? _GEN_147 : _T_4411_0_19;
  assign _GEN_180 = _T_341_0 ? _GEN_148 : _T_4411_0_20;
  assign _GEN_181 = _T_341_0 ? _GEN_149 : _T_4411_0_21;
  assign _GEN_182 = _T_341_0 ? _GEN_150 : _T_4411_0_22;
  assign _GEN_183 = _T_341_0 ? _GEN_151 : _T_4411_0_23;
  assign _GEN_184 = _T_341_0 ? _GEN_152 : _T_4411_0_24;
  assign _GEN_185 = _T_341_0 ? _GEN_153 : _T_4411_0_25;
  assign _GEN_186 = _T_341_0 ? _GEN_154 : _T_4411_0_26;
  assign _GEN_187 = _T_341_0 ? _GEN_155 : _T_4411_0_27;
  assign _GEN_188 = _T_341_0 ? _GEN_156 : _T_4411_0_28;
  assign _GEN_189 = _T_341_0 ? _GEN_157 : _T_4411_0_29;
  assign _GEN_190 = _T_341_0 ? _GEN_158 : _T_4411_0_30;
  assign _GEN_191 = _T_341_0 ? _GEN_159 : _T_4411_0_31;
  assign _T_4670 = _T_4302 & _T_335_0;
  assign _T_4673 = _T_4670 & _T_4330;
  assign _T_4679 = _T_4673 & _T_4656;
  assign _GEN_192 = _T_4679 ? 1'h1 : _T_4321_0;
  assign _GEN_193 = _T_4679 ? _T_329_0 : _T_4334_0;
  assign _T_4688 = _T_4643 == 1'h0;
  assign _T_4689 = _T_4321_0 & _T_4688;
  assign _T_4794 = {_T_4411_0_1,_T_4411_0_0};
  assign _T_4795 = {_T_4411_0_3,_T_4411_0_2};
  assign _T_4796 = {_T_4795,_T_4794};
  assign _T_4797 = {_T_4411_0_5,_T_4411_0_4};
  assign _T_4798 = {_T_4411_0_7,_T_4411_0_6};
  assign _T_4799 = {_T_4798,_T_4797};
  assign _T_4800 = {_T_4799,_T_4796};
  assign _T_4801 = {_T_4411_0_9,_T_4411_0_8};
  assign _T_4802 = {_T_4411_0_11,_T_4411_0_10};
  assign _T_4803 = {_T_4802,_T_4801};
  assign _T_4804 = {_T_4411_0_13,_T_4411_0_12};
  assign _T_4805 = {_T_4411_0_15,_T_4411_0_14};
  assign _T_4806 = {_T_4805,_T_4804};
  assign _T_4807 = {_T_4806,_T_4803};
  assign _T_4808 = {_T_4807,_T_4800};
  assign _T_4809 = {_T_4411_0_17,_T_4411_0_16};
  assign _T_4810 = {_T_4411_0_19,_T_4411_0_18};
  assign _T_4811 = {_T_4810,_T_4809};
  assign _T_4812 = {_T_4411_0_21,_T_4411_0_20};
  assign _T_4813 = {_T_4411_0_23,_T_4411_0_22};
  assign _T_4814 = {_T_4813,_T_4812};
  assign _T_4815 = {_T_4814,_T_4811};
  assign _T_4816 = {_T_4411_0_25,_T_4411_0_24};
  assign _T_4817 = {_T_4411_0_27,_T_4411_0_26};
  assign _T_4818 = {_T_4817,_T_4816};
  assign _T_4819 = {_T_4411_0_29,_T_4411_0_28};
  assign _T_4820 = {_T_4411_0_31,_T_4411_0_30};
  assign _T_4821 = {_T_4820,_T_4819};
  assign _T_4822 = {_T_4821,_T_4818};
  assign _T_4823 = {_T_4822,_T_4815};
  assign _T_4824 = {_T_4823,_T_4808};
  assign _T_4825 = _T_4824[511:0];
  assign _T_4828 = _T_4824[1023:512];
  assign _T_4829 = _T_4649 ? _T_4828 : _T_4825;
  assign _T_4836 = io_axi_outputMemAddrValids_0 & io_axi_outputMemAddrReadys_0;
  assign _T_4843 = _T_4652 & _T_4330;
  assign _T_4844 = _T_4836 | _T_4843;
  assign _T_4846 = _T_4640 == 1'h0;
  assign _T_4849 = _T_4640 + 1'h1;
  assign _T_4850 = _T_4849[0:0];
  assign _GEN_194 = _T_4846 ? 1'h1 : _T_4643;
  assign _GEN_195 = _T_4846 ? _T_4640 : _T_4850;
  assign _GEN_196 = _T_4844 ? _GEN_194 : _T_4643;
  assign _GEN_197 = _T_4844 ? _GEN_195 : _T_4640;
  assign _T_4851 = io_axi_outputMemBlockValids_0 & io_axi_outputMemBlockReadys_0;
  assign _T_4856 = _T_4649 + 1'h1;
  assign _T_4857 = _T_4856[0:0];
  assign _T_4858 = _T_4649 ? 1'h0 : _T_4857;
  assign _GEN_198 = _T_4851 ? _T_4858 : _T_4649;
  assign _T_4862 = _T_4851 & _T_4649;
  assign _T_4869 = _T_4646 < _T_4640;
  assign _T_4870 = _T_4869 | _T_4643;
  assign _T_4871 = _T_4330 & _T_4870;
  assign _T_4872 = _T_4862 | _T_4871;
  assign _T_4874 = _T_4646 == 1'h0;
  assign _T_4879 = _T_256 + 1'h1;
  assign _T_4880 = _T_4879[0:0];
  assign _T_4881 = _T_256 ? 1'h0 : _T_4880;
  assign _T_4890 = _T_4646 + 1'h1;
  assign _T_4891 = _T_4890[0:0];
  assign _GEN_199 = _T_4874 ? _T_4881 : _T_256;
  assign _GEN_200 = _T_4874 ? 1'h0 : _GEN_125;
  assign _GEN_201 = _T_4874 ? 1'h0 : _T_4891;
  assign _GEN_202 = _T_4874 ? 1'h0 : _GEN_197;
  assign _GEN_203 = _T_4874 ? 1'h0 : _GEN_196;
  assign _GEN_204 = _T_4874 ? 1'h0 : _GEN_126;
  assign _GEN_205 = _T_4874 ? 1'h0 : _GEN_192;
  assign _GEN_206 = _T_4874 ? 1'h0 : _GEN_127;
  assign _GEN_207 = _T_4872 ? _GEN_199 : _T_256;
  assign _GEN_208 = _T_4872 ? _GEN_200 : _GEN_125;
  assign _GEN_209 = _T_4872 ? _GEN_201 : _T_4646;
  assign _GEN_210 = _T_4872 ? _GEN_202 : _GEN_197;
  assign _GEN_211 = _T_4872 ? _GEN_203 : _GEN_196;
  assign _GEN_212 = _T_4872 ? _GEN_204 : _GEN_126;
  assign _GEN_213 = _T_4872 ? _GEN_205 : _GEN_192;
  assign _GEN_214 = _T_4872 ? _GEN_206 : _GEN_127;
  assign _T_4893 = _T_256 == 1'h0;
  assign _T_4907 = _T_4893 ? _T_4679 : 1'h0;
  assign _T_4923 = _T_256 ? _T_4679 : 1'h0;
  assign cumFinished = _T_359_0 & _T_4330;
  assign io_axi_inputMemAddrs_0 = {{32'd0}, _T_277_0};
  assign io_axi_inputMemAddrValids_0 = _T_760;
  assign io_axi_inputMemAddrLens_0 = 8'h1;
  assign io_axi_inputMemBlockReadys_0 = _T_4265;
  assign io_axi_outputMemAddrs_0 = {{32'd0}, _T_4334_0};
  assign io_axi_outputMemAddrValids_0 = _T_4689;
  assign io_axi_outputMemAddrLens_0 = 8'h1;
  assign io_axi_outputMemAddrIds_0 = {{15'd0}, _T_4640};
  assign io_axi_outputMemBlocks_0 = _T_4829;
  assign io_axi_outputMemBlockValids_0 = _T_4630_0;
  assign io_axi_outputMemBlockLasts_0 = _T_4649;
  assign io_axi_finished = cumFinished;
  assign io_streamingCores_0_metadataPtr = 32'h0;
  assign io_streamingCores_0_inputMemBlock = _GEN_0;
  assign io_streamingCores_0_inputMemIdx = _T_700_0;
  assign io_streamingCores_0_inputMemBlockValid = _T_4282;
  assign io_streamingCores_0_outputMemAddrReady = _T_4907;
  assign io_streamingCores_1_metadataPtr = 32'h80;
  assign io_streamingCores_1_inputMemBlock = _GEN_1;
  assign io_streamingCores_1_inputMemIdx = _T_700_0;
  assign io_streamingCores_1_inputMemBlockValid = _T_4299;
  assign io_streamingCores_1_outputMemAddrReady = _T_4923;
  assign _T_277_0 = _T_258;
  assign _T_283_0 = _T_260;
  assign _T_289_0 = _T_262;
  assign _T_329_0 = _T_294;
  assign _T_335_0 = _T_296;
  assign _T_341_0 = _T_298;
  assign _T_347_0 = _T_300;
  assign _T_353_0 = _T_302;
  assign _T_359_0 = _T_305;
  assign _GEN_0 = _GEN_124;
  assign _GEN_1 = _GEN_124;
  assign _GEN_2 = _T_347_0;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{$random}};
  _T_250 = _RAND_0[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{$random}};
  _T_253 = _RAND_1[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{$random}};
  _T_256 = _RAND_2[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{$random}};
  _T_258 = _RAND_3[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_4 = {1{$random}};
  _T_260 = _RAND_4[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_5 = {1{$random}};
  _T_262 = _RAND_5[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_6 = {1{$random}};
  _T_294 = _RAND_6[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_7 = {1{$random}};
  _T_296 = _RAND_7[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_8 = {1{$random}};
  _T_298 = _RAND_8[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_9 = {1{$random}};
  _T_300 = _RAND_9[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_10 = {1{$random}};
  _T_302 = _RAND_10[4:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_11 = {1{$random}};
  _T_305 = _RAND_11[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_12 = {1{$random}};
  _T_365 = _RAND_12[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_13 = {1{$random}};
  _T_379_0 = _RAND_13[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_14 = {1{$random}};
  _T_400_0 = _RAND_14[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_15 = {1{$random}};
  _T_481_0_0 = _RAND_15[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_16 = {1{$random}};
  _T_481_0_1 = _RAND_16[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_17 = {1{$random}};
  _T_481_0_2 = _RAND_17[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_18 = {1{$random}};
  _T_481_0_3 = _RAND_18[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_19 = {1{$random}};
  _T_481_0_4 = _RAND_19[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_20 = {1{$random}};
  _T_481_0_5 = _RAND_20[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_21 = {1{$random}};
  _T_481_0_6 = _RAND_21[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_22 = {1{$random}};
  _T_481_0_7 = _RAND_22[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_23 = {1{$random}};
  _T_481_0_8 = _RAND_23[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_24 = {1{$random}};
  _T_481_0_9 = _RAND_24[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_25 = {1{$random}};
  _T_481_0_10 = _RAND_25[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_26 = {1{$random}};
  _T_481_0_11 = _RAND_26[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_27 = {1{$random}};
  _T_481_0_12 = _RAND_27[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_28 = {1{$random}};
  _T_481_0_13 = _RAND_28[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_29 = {1{$random}};
  _T_481_0_14 = _RAND_29[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_30 = {1{$random}};
  _T_481_0_15 = _RAND_30[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_31 = {1{$random}};
  _T_481_0_16 = _RAND_31[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_32 = {1{$random}};
  _T_481_0_17 = _RAND_32[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_33 = {1{$random}};
  _T_481_0_18 = _RAND_33[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_34 = {1{$random}};
  _T_481_0_19 = _RAND_34[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_35 = {1{$random}};
  _T_481_0_20 = _RAND_35[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_36 = {1{$random}};
  _T_481_0_21 = _RAND_36[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_37 = {1{$random}};
  _T_481_0_22 = _RAND_37[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_38 = {1{$random}};
  _T_481_0_23 = _RAND_38[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_39 = {1{$random}};
  _T_481_0_24 = _RAND_39[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_40 = {1{$random}};
  _T_481_0_25 = _RAND_40[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_41 = {1{$random}};
  _T_481_0_26 = _RAND_41[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_42 = {1{$random}};
  _T_481_0_27 = _RAND_42[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_43 = {1{$random}};
  _T_481_0_28 = _RAND_43[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_44 = {1{$random}};
  _T_481_0_29 = _RAND_44[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_45 = {1{$random}};
  _T_481_0_30 = _RAND_45[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_46 = {1{$random}};
  _T_481_0_31 = _RAND_46[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_47 = {1{$random}};
  _T_700_0 = _RAND_47[4:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_48 = {1{$random}};
  _T_721_0 = _RAND_48[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_49 = {1{$random}};
  _T_731 = _RAND_49[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_50 = {1{$random}};
  _T_734 = _RAND_50[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_51 = {1{$random}};
  _T_737 = _RAND_51[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_52 = {1{$random}};
  _T_4302 = _RAND_52[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_53 = {1{$random}};
  _T_4321_0 = _RAND_53[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_54 = {1{$random}};
  _T_4334_0 = _RAND_54[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_55 = {1{$random}};
  _T_4411_0_0 = _RAND_55[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_56 = {1{$random}};
  _T_4411_0_1 = _RAND_56[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_57 = {1{$random}};
  _T_4411_0_2 = _RAND_57[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_58 = {1{$random}};
  _T_4411_0_3 = _RAND_58[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_59 = {1{$random}};
  _T_4411_0_4 = _RAND_59[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_60 = {1{$random}};
  _T_4411_0_5 = _RAND_60[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_61 = {1{$random}};
  _T_4411_0_6 = _RAND_61[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_62 = {1{$random}};
  _T_4411_0_7 = _RAND_62[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_63 = {1{$random}};
  _T_4411_0_8 = _RAND_63[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_64 = {1{$random}};
  _T_4411_0_9 = _RAND_64[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_65 = {1{$random}};
  _T_4411_0_10 = _RAND_65[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_66 = {1{$random}};
  _T_4411_0_11 = _RAND_66[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_67 = {1{$random}};
  _T_4411_0_12 = _RAND_67[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_68 = {1{$random}};
  _T_4411_0_13 = _RAND_68[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_69 = {1{$random}};
  _T_4411_0_14 = _RAND_69[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_70 = {1{$random}};
  _T_4411_0_15 = _RAND_70[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_71 = {1{$random}};
  _T_4411_0_16 = _RAND_71[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_72 = {1{$random}};
  _T_4411_0_17 = _RAND_72[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_73 = {1{$random}};
  _T_4411_0_18 = _RAND_73[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_74 = {1{$random}};
  _T_4411_0_19 = _RAND_74[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_75 = {1{$random}};
  _T_4411_0_20 = _RAND_75[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_76 = {1{$random}};
  _T_4411_0_21 = _RAND_76[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_77 = {1{$random}};
  _T_4411_0_22 = _RAND_77[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_78 = {1{$random}};
  _T_4411_0_23 = _RAND_78[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_79 = {1{$random}};
  _T_4411_0_24 = _RAND_79[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_80 = {1{$random}};
  _T_4411_0_25 = _RAND_80[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_81 = {1{$random}};
  _T_4411_0_26 = _RAND_81[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_82 = {1{$random}};
  _T_4411_0_27 = _RAND_82[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_83 = {1{$random}};
  _T_4411_0_28 = _RAND_83[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_84 = {1{$random}};
  _T_4411_0_29 = _RAND_84[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_85 = {1{$random}};
  _T_4411_0_30 = _RAND_85[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_86 = {1{$random}};
  _T_4411_0_31 = _RAND_86[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_87 = {1{$random}};
  _T_4630_0 = _RAND_87[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_88 = {1{$random}};
  _T_4640 = _RAND_88[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_89 = {1{$random}};
  _T_4643 = _RAND_89[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_90 = {1{$random}};
  _T_4646 = _RAND_90[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_91 = {1{$random}};
  _T_4649 = _RAND_91[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_92 = {1{$random}};
  _T_4652 = _RAND_92[0:0];
  `endif // RANDOMIZE_REG_INIT
  end
`endif // RANDOMIZE
  always @(posedge clock) begin
    if (reset) begin
      _T_250 <= 1'h0;
    end else begin
      if (_T_775) begin
        if (_T_787) begin
          if (_T_250) begin
            _T_250 <= 1'h0;
          end else begin
            _T_250 <= _T_793;
          end
        end
      end
    end
    if (reset) begin
      _T_253 <= 1'h0;
    end else begin
      if (_T_4231) begin
        if (_T_4243) begin
          if (_T_253) begin
            _T_253 <= 1'h0;
          end else begin
            _T_253 <= _T_4249;
          end
        end
      end
    end
    if (reset) begin
      _T_256 <= 1'h0;
    end else begin
      if (_T_4872) begin
        if (_T_4874) begin
          if (_T_256) begin
            _T_256 <= 1'h0;
          end else begin
            _T_256 <= _T_4880;
          end
        end
      end
    end
    if (_T_250) begin
      _T_258 <= io_streamingCores_1_inputMemAddr;
    end else begin
      _T_258 <= io_streamingCores_0_inputMemAddr;
    end
    if (_T_250) begin
      _T_260 <= io_streamingCores_1_inputMemAddrValid;
    end else begin
      _T_260 <= io_streamingCores_0_inputMemAddrValid;
    end
    if (_T_250) begin
      _T_262 <= io_streamingCores_1_inputMemAddrsFinished;
    end else begin
      _T_262 <= io_streamingCores_0_inputMemAddrsFinished;
    end
    if (_T_256) begin
      _T_294 <= io_streamingCores_1_outputMemAddr;
    end else begin
      _T_294 <= io_streamingCores_0_outputMemAddr;
    end
    if (_T_256) begin
      _T_296 <= io_streamingCores_1_outputMemAddrValid;
    end else begin
      _T_296 <= io_streamingCores_0_outputMemAddrValid;
    end
    if (_T_256) begin
      _T_298 <= io_streamingCores_1_outputMemBlockValid;
    end else begin
      _T_298 <= io_streamingCores_0_outputMemBlockValid;
    end
    if (_T_256) begin
      _T_300 <= io_streamingCores_1_outputMemBlock;
    end else begin
      _T_300 <= io_streamingCores_0_outputMemBlock;
    end
    if (_T_256) begin
      _T_302 <= io_streamingCores_1_outputMemIdx;
    end else begin
      _T_302 <= io_streamingCores_0_outputMemIdx;
    end
    if (reset) begin
      _T_305 <= 1'h0;
    end else begin
      _T_305 <= _T_326;
    end
    if (reset) begin
      _T_365 <= 1'h0;
    end else begin
      if (_T_775) begin
        if (_T_787) begin
          _T_365 <= 1'h0;
        end else begin
          if (_T_743) begin
            _T_365 <= _T_746;
          end
        end
      end else begin
        if (_T_743) begin
          _T_365 <= _T_746;
        end
      end
    end
    if (reset) begin
      _T_379_0 <= 1'h0;
    end else begin
      if (_T_4231) begin
        if (io_axi_inputMemBlockReadys_0) begin
          if (_T_721_0) begin
            if (_T_801) begin
              _T_379_0 <= 1'h0;
            end else begin
              if (_T_775) begin
                _T_379_0 <= 1'h1;
              end
            end
          end else begin
            if (_T_775) begin
              _T_379_0 <= 1'h1;
            end
          end
        end else begin
          _T_379_0 <= 1'h0;
        end
      end else begin
        if (_T_721_0) begin
          if (_T_801) begin
            _T_379_0 <= 1'h0;
          end else begin
            if (_T_775) begin
              _T_379_0 <= 1'h1;
            end
          end
        end else begin
          if (_T_775) begin
            _T_379_0 <= 1'h1;
          end
        end
      end
    end
    if (reset) begin
      _T_400_0 <= 1'h0;
    end else begin
      if (_T_721_0) begin
        if (_T_801) begin
          _T_400_0 <= 1'h0;
        end else begin
          if (_T_775) begin
            if (io_axi_inputMemAddrValids_0) begin
              _T_400_0 <= 1'h1;
            end
          end
        end
      end else begin
        if (_T_775) begin
          if (io_axi_inputMemAddrValids_0) begin
            _T_400_0 <= 1'h1;
          end
        end
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_0 <= _T_922;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_1 <= _T_1028;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_2 <= _T_1134;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_3 <= _T_1240;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_4 <= _T_1346;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_5 <= _T_1452;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_6 <= _T_1558;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_7 <= _T_1664;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_8 <= _T_1770;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_9 <= _T_1876;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_10 <= _T_1982;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_11 <= _T_2088;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_12 <= _T_2194;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_13 <= _T_2300;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_14 <= _T_2406;
      end
    end
    if (_T_809) begin
      if (_T_818) begin
        _T_481_0_15 <= _T_2512;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_16 <= _T_922;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_17 <= _T_1028;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_18 <= _T_1134;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_19 <= _T_1240;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_20 <= _T_1346;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_21 <= _T_1452;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_22 <= _T_1558;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_23 <= _T_1664;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_24 <= _T_1770;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_25 <= _T_1876;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_26 <= _T_1982;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_27 <= _T_2088;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_28 <= _T_2194;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_29 <= _T_2300;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_30 <= _T_2406;
      end
    end
    if (_T_809) begin
      if (_T_737) begin
        _T_481_0_31 <= _T_2512;
      end
    end
    if (reset) begin
      _T_700_0 <= 5'h0;
    end else begin
      if (_T_721_0) begin
        if (_T_801) begin
          _T_700_0 <= 5'h0;
        end else begin
          _T_700_0 <= _T_808;
        end
      end
    end
    if (reset) begin
      _T_721_0 <= 1'h0;
    end else begin
      if (_T_4231) begin
        if (io_axi_inputMemBlockReadys_0) begin
          _T_721_0 <= 1'h1;
        end else begin
          if (_T_721_0) begin
            if (_T_801) begin
              _T_721_0 <= 1'h0;
            end
          end
        end
      end else begin
        if (_T_721_0) begin
          if (_T_801) begin
            _T_721_0 <= 1'h0;
          end
        end
      end
    end
    if (reset) begin
      _T_731 <= 1'h0;
    end else begin
      if (_T_775) begin
        if (_T_787) begin
          _T_731 <= 1'h0;
        end else begin
          _T_731 <= _T_799;
        end
      end
    end
    if (reset) begin
      _T_734 <= 1'h0;
    end else begin
      if (_T_4231) begin
        if (_T_4243) begin
          _T_734 <= 1'h0;
        end else begin
          _T_734 <= _T_4254;
        end
      end
    end
    if (reset) begin
      _T_737 <= 1'h0;
    end else begin
      if (_T_809) begin
        if (_T_737) begin
          _T_737 <= 1'h0;
        end else begin
          _T_737 <= _T_815;
        end
      end
    end
    if (reset) begin
      _T_4302 <= 1'h0;
    end else begin
      if (_T_4872) begin
        if (_T_4874) begin
          _T_4302 <= 1'h0;
        end else begin
          if (_T_4304) begin
            _T_4302 <= _T_4307;
          end
        end
      end else begin
        if (_T_4304) begin
          _T_4302 <= _T_4307;
        end
      end
    end
    if (reset) begin
      _T_4321_0 <= 1'h0;
    end else begin
      if (_T_4872) begin
        if (_T_4874) begin
          _T_4321_0 <= 1'h0;
        end else begin
          if (_T_4679) begin
            _T_4321_0 <= 1'h1;
          end
        end
      end else begin
        if (_T_4679) begin
          _T_4321_0 <= 1'h1;
        end
      end
    end
    if (_T_4679) begin
      _T_4334_0 <= _T_329_0;
    end
    if (_T_341_0) begin
      if (5'h0 == _T_353_0) begin
        _T_4411_0_0 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h1 == _T_353_0) begin
        _T_4411_0_1 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h2 == _T_353_0) begin
        _T_4411_0_2 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h3 == _T_353_0) begin
        _T_4411_0_3 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h4 == _T_353_0) begin
        _T_4411_0_4 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h5 == _T_353_0) begin
        _T_4411_0_5 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h6 == _T_353_0) begin
        _T_4411_0_6 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h7 == _T_353_0) begin
        _T_4411_0_7 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h8 == _T_353_0) begin
        _T_4411_0_8 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h9 == _T_353_0) begin
        _T_4411_0_9 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'ha == _T_353_0) begin
        _T_4411_0_10 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'hb == _T_353_0) begin
        _T_4411_0_11 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'hc == _T_353_0) begin
        _T_4411_0_12 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'hd == _T_353_0) begin
        _T_4411_0_13 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'he == _T_353_0) begin
        _T_4411_0_14 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'hf == _T_353_0) begin
        _T_4411_0_15 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h10 == _T_353_0) begin
        _T_4411_0_16 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h11 == _T_353_0) begin
        _T_4411_0_17 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h12 == _T_353_0) begin
        _T_4411_0_18 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h13 == _T_353_0) begin
        _T_4411_0_19 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h14 == _T_353_0) begin
        _T_4411_0_20 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h15 == _T_353_0) begin
        _T_4411_0_21 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h16 == _T_353_0) begin
        _T_4411_0_22 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h17 == _T_353_0) begin
        _T_4411_0_23 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h18 == _T_353_0) begin
        _T_4411_0_24 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h19 == _T_353_0) begin
        _T_4411_0_25 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h1a == _T_353_0) begin
        _T_4411_0_26 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h1b == _T_353_0) begin
        _T_4411_0_27 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h1c == _T_353_0) begin
        _T_4411_0_28 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h1d == _T_353_0) begin
        _T_4411_0_29 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h1e == _T_353_0) begin
        _T_4411_0_30 <= _GEN_2;
      end
    end
    if (_T_341_0) begin
      if (5'h1f == _T_353_0) begin
        _T_4411_0_31 <= _GEN_2;
      end
    end
    if (reset) begin
      _T_4630_0 <= 1'h0;
    end else begin
      if (_T_4872) begin
        if (_T_4874) begin
          _T_4630_0 <= 1'h0;
        end else begin
          if (_T_4664) begin
            _T_4630_0 <= 1'h1;
          end
        end
      end else begin
        if (_T_4664) begin
          _T_4630_0 <= 1'h1;
        end
      end
    end
    if (reset) begin
      _T_4640 <= 1'h0;
    end else begin
      if (_T_4872) begin
        if (_T_4874) begin
          _T_4640 <= 1'h0;
        end else begin
          if (_T_4844) begin
            if (!(_T_4846)) begin
              _T_4640 <= _T_4850;
            end
          end
        end
      end else begin
        if (_T_4844) begin
          if (!(_T_4846)) begin
            _T_4640 <= _T_4850;
          end
        end
      end
    end
    if (reset) begin
      _T_4643 <= 1'h0;
    end else begin
      if (_T_4872) begin
        if (_T_4874) begin
          _T_4643 <= 1'h0;
        end else begin
          if (_T_4844) begin
            if (_T_4846) begin
              _T_4643 <= 1'h1;
            end
          end
        end
      end else begin
        if (_T_4844) begin
          if (_T_4846) begin
            _T_4643 <= 1'h1;
          end
        end
      end
    end
    if (reset) begin
      _T_4646 <= 1'h0;
    end else begin
      if (_T_4872) begin
        if (_T_4874) begin
          _T_4646 <= 1'h0;
        end else begin
          _T_4646 <= _T_4891;
        end
      end
    end
    if (reset) begin
      _T_4649 <= 1'h0;
    end else begin
      if (_T_4851) begin
        if (_T_4649) begin
          _T_4649 <= 1'h0;
        end else begin
          _T_4649 <= _T_4857;
        end
      end
    end
    if (reset) begin
      _T_4652 <= 1'h0;
    end else begin
      if (_T_4872) begin
        if (_T_4874) begin
          _T_4652 <= 1'h0;
        end else begin
          if (_T_4657) begin
            _T_4652 <= 1'h1;
          end
        end
      end else begin
        if (_T_4657) begin
          _T_4652 <= 1'h1;
        end
      end
    end
  end
endmodule
module PassThrough(
  input         clock,
  input         reset,
  input  [31:0] io_inputWord,
  input         io_inputValid,
  input         io_inputFinished,
  output        io_inputReady,
  output [31:0] io_outputWord,
  output        io_outputValid,
  output        io_outputFinished,
  input         io_outputReady
);
  reg [31:0] _T_11;
  reg [31:0] _RAND_0;
  reg  _T_14;
  reg [31:0] _RAND_1;
  reg  _T_17;
  reg [31:0] _RAND_2;
  wire  _T_21;
  wire  _T_23;
  wire  _T_24;
  wire  _T_25;
  reg  _T_31;
  reg [31:0] _RAND_3;
  wire  _T_35;
  wire  _T_37;
  wire  _T_38;
  wire  _T_41;
  wire  _T_45;
  wire  _T_46;
  wire [1:0] _T_48;
  wire  _T_49;
  wire  _GEN_0;
  wire  _T_55;
  wire  _T_56;
  wire  _T_57;
  wire [31:0] _GEN_1;
  wire  _GEN_2;
  wire  _GEN_3;
  wire  _GEN_4;
  wire  _T_67;
  wire [31:0] _GEN_10;
  wire  _GEN_11;
  wire  _GEN_12;
  wire  _GEN_13;
  wire [31:0] _GEN_14;
  wire  _GEN_15;
  wire  _GEN_16;
  wire  _GEN_17;
  wire  _T_86;
  wire  _T_91;
  assign _T_23 = io_outputValid == 1'h0;
  assign _T_24 = _T_23 | io_outputReady;
  assign _T_25 = _T_21 & _T_24;
  assign _T_35 = _T_31 == 1'h0;
  assign _T_37 = _T_14 == 1'h0;
  assign _T_38 = _T_35 & _T_37;
  assign _T_41 = _T_38 | _T_25;
  assign _T_45 = _T_21 == 1'h0;
  assign _T_46 = _T_31 & _T_45;
  assign _T_48 = _T_31 + 1'h1;
  assign _T_49 = _T_48[0:0];
  assign _GEN_0 = _T_46 ? _T_49 : _T_31;
  assign _T_55 = _T_17 == 1'h0;
  assign _T_56 = io_inputFinished & _T_55;
  assign _T_57 = _T_56 | io_inputValid;
  assign _GEN_1 = _T_37 ? io_inputWord : _T_11;
  assign _GEN_2 = _T_37 ? io_inputFinished : _T_17;
  assign _GEN_3 = _T_37 ? _T_57 : _T_14;
  assign _GEN_4 = _T_37 ? _T_57 : 1'h1;
  assign _GEN_10 = _T_25 ? io_inputWord : _T_11;
  assign _GEN_11 = _T_25 ? io_inputFinished : _T_17;
  assign _GEN_12 = _T_25 ? _T_57 : _T_14;
  assign _GEN_13 = _T_25 ? _T_67 : _GEN_0;
  assign _GEN_14 = _T_35 ? _GEN_1 : _GEN_10;
  assign _GEN_15 = _T_35 ? _GEN_2 : _GEN_11;
  assign _GEN_16 = _T_35 ? _GEN_3 : _GEN_12;
  assign _GEN_17 = _T_35 ? _GEN_4 : _GEN_13;
  assign _T_86 = _T_17 & _T_35;
  assign _T_91 = _T_21 & _T_55;
  assign io_inputReady = _T_41;
  assign io_outputWord = _T_11;
  assign io_outputValid = _T_91;
  assign io_outputFinished = _T_86;
  assign _T_21 = _T_31;
  assign _T_67 = _T_57;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{$random}};
  _T_11 = _RAND_0[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{$random}};
  _T_14 = _RAND_1[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{$random}};
  _T_17 = _RAND_2[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{$random}};
  _T_31 = _RAND_3[0:0];
  `endif // RANDOMIZE_REG_INIT
  end
`endif // RANDOMIZE
  always @(posedge clock) begin
    if (_T_35) begin
      if (_T_37) begin
        _T_11 <= io_inputWord;
      end
    end else begin
      if (_T_25) begin
        _T_11 <= io_inputWord;
      end
    end
    if (reset) begin
      _T_14 <= 1'h0;
    end else begin
      if (_T_35) begin
        if (_T_37) begin
          _T_14 <= _T_57;
        end
      end else begin
        if (_T_25) begin
          _T_14 <= _T_57;
        end
      end
    end
    if (reset) begin
      _T_17 <= 1'h0;
    end else begin
      if (_T_35) begin
        if (_T_37) begin
          _T_17 <= io_inputFinished;
        end
      end else begin
        if (_T_25) begin
          _T_17 <= io_inputFinished;
        end
      end
    end
    if (reset) begin
      _T_31 <= 1'h0;
    end else begin
      if (_T_35) begin
        if (_T_37) begin
          _T_31 <= _T_57;
        end else begin
          _T_31 <= 1'h1;
        end
      end else begin
        if (_T_25) begin
          _T_31 <= _T_67;
        end else begin
          if (_T_46) begin
            _T_31 <= _T_49;
          end
        end
      end
    end
  end
endmodule
module DualPortBRAM(
  input         clock,
  input  [4:0]  io_a_addr,
  input  [31:0] io_a_din,
  input         io_a_wr,
  input  [4:0]  io_b_addr,
  output [31:0] io_b_dout
);
  reg [31:0] mem [0:31];
  reg [31:0] _RAND_0;
  wire [31:0] mem__T_16_data;
  wire [4:0] mem__T_16_addr;
  wire [31:0] mem__T_22_data;
  wire [4:0] mem__T_22_addr;
  wire [31:0] mem__T_17_data;
  wire [4:0] mem__T_17_addr;
  wire  mem__T_17_mask;
  wire  mem__T_17_en;
  wire [31:0] mem__T_23_data;
  wire [4:0] mem__T_23_addr;
  wire  mem__T_23_mask;
  wire  mem__T_23_en;
  reg [4:0] regAddrA;
  reg [31:0] _RAND_1;
  reg [4:0] regAddrB;
  reg [31:0] _RAND_2;
  assign mem__T_16_addr = regAddrA;
  assign mem__T_16_data = mem[mem__T_16_addr];
  assign mem__T_22_addr = regAddrB;
  assign mem__T_22_data = mem[mem__T_22_addr];
  assign mem__T_17_data = io_a_din;
  assign mem__T_17_addr = io_a_addr;
  assign mem__T_17_mask = io_a_wr;
  assign mem__T_17_en = io_a_wr;
  assign mem__T_23_data = 32'h0;
  assign mem__T_23_addr = io_b_addr;
  assign mem__T_23_mask = 1'h0;
  assign mem__T_23_en = 1'h0;
  assign io_b_dout = mem__T_22_data;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  _RAND_0 = {1{$random}};
  `ifdef RANDOMIZE_MEM_INIT
  for (initvar = 0; initvar < 32; initvar = initvar+1)
    mem[initvar] = _RAND_0[31:0];
  `endif // RANDOMIZE_MEM_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{$random}};
  regAddrA = _RAND_1[4:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{$random}};
  regAddrB = _RAND_2[4:0];
  `endif // RANDOMIZE_REG_INIT
  end
`endif // RANDOMIZE
  always @(posedge clock) begin
    if(mem__T_17_en & mem__T_17_mask) begin
      mem[mem__T_17_addr] <= mem__T_17_data;
    end
    if(mem__T_23_en & mem__T_23_mask) begin
      mem[mem__T_23_addr] <= mem__T_23_data;
    end
    regAddrA <= io_a_addr;
    regAddrB <= io_b_addr;
  end
endmodule
module InnerCore(
  input         clock,
  input         reset,
  input  [31:0] io_inputMemBlock,
  input  [4:0]  io_inputMemIdx,
  input         io_inputMemBlockValid,
  input  [10:0] io_inputBits,
  output        io_inputMemConsumed,
  input         io_inputFinished,
  output [31:0] io_outputMemBlock,
  output        io_outputMemBlockValid,
  input         io_outputMemBlockReady,
  output [10:0] io_outputBits,
  output        io_outputFinished
);
  wire  inner_clock;
  wire  inner_reset;
  wire [31:0] inner_io_inputWord;
  wire  inner_io_inputValid;
  wire  inner_io_inputFinished;
  wire  inner_io_inputReady;
  wire [31:0] inner_io_outputWord;
  wire  inner_io_outputValid;
  wire  inner_io_outputFinished;
  wire  inner_io_outputReady;
  reg  inputMemBlock_0;
  reg [31:0] _RAND_0;
  reg  inputMemBlock_1;
  reg [31:0] _RAND_1;
  reg  inputMemBlock_2;
  reg [31:0] _RAND_2;
  reg  inputMemBlock_3;
  reg [31:0] _RAND_3;
  reg  inputMemBlock_4;
  reg [31:0] _RAND_4;
  reg  inputMemBlock_5;
  reg [31:0] _RAND_5;
  reg  inputMemBlock_6;
  reg [31:0] _RAND_6;
  reg  inputMemBlock_7;
  reg [31:0] _RAND_7;
  reg  inputMemBlock_8;
  reg [31:0] _RAND_8;
  reg  inputMemBlock_9;
  reg [31:0] _RAND_9;
  reg  inputMemBlock_10;
  reg [31:0] _RAND_10;
  reg  inputMemBlock_11;
  reg [31:0] _RAND_11;
  reg  inputMemBlock_12;
  reg [31:0] _RAND_12;
  reg  inputMemBlock_13;
  reg [31:0] _RAND_13;
  reg  inputMemBlock_14;
  reg [31:0] _RAND_14;
  reg  inputMemBlock_15;
  reg [31:0] _RAND_15;
  reg  inputMemBlock_16;
  reg [31:0] _RAND_16;
  reg  inputMemBlock_17;
  reg [31:0] _RAND_17;
  reg  inputMemBlock_18;
  reg [31:0] _RAND_18;
  reg  inputMemBlock_19;
  reg [31:0] _RAND_19;
  reg  inputMemBlock_20;
  reg [31:0] _RAND_20;
  reg  inputMemBlock_21;
  reg [31:0] _RAND_21;
  reg  inputMemBlock_22;
  reg [31:0] _RAND_22;
  reg  inputMemBlock_23;
  reg [31:0] _RAND_23;
  reg  inputMemBlock_24;
  reg [31:0] _RAND_24;
  reg  inputMemBlock_25;
  reg [31:0] _RAND_25;
  reg  inputMemBlock_26;
  reg [31:0] _RAND_26;
  reg  inputMemBlock_27;
  reg [31:0] _RAND_27;
  reg  inputMemBlock_28;
  reg [31:0] _RAND_28;
  reg  inputMemBlock_29;
  reg [31:0] _RAND_29;
  reg  inputMemBlock_30;
  reg [31:0] _RAND_30;
  reg  inputMemBlock_31;
  reg [31:0] _RAND_31;
  reg [5:0] inputPieceBitsRemaining;
  reg [31:0] _RAND_32;
  reg [10:0] inputBitsRemaining;
  reg [31:0] _RAND_33;
  reg  outputMemBlock_0;
  reg [31:0] _RAND_34;
  reg  outputMemBlock_1;
  reg [31:0] _RAND_35;
  reg  outputMemBlock_2;
  reg [31:0] _RAND_36;
  reg  outputMemBlock_3;
  reg [31:0] _RAND_37;
  reg  outputMemBlock_4;
  reg [31:0] _RAND_38;
  reg  outputMemBlock_5;
  reg [31:0] _RAND_39;
  reg  outputMemBlock_6;
  reg [31:0] _RAND_40;
  reg  outputMemBlock_7;
  reg [31:0] _RAND_41;
  reg  outputMemBlock_8;
  reg [31:0] _RAND_42;
  reg  outputMemBlock_9;
  reg [31:0] _RAND_43;
  reg  outputMemBlock_10;
  reg [31:0] _RAND_44;
  reg  outputMemBlock_11;
  reg [31:0] _RAND_45;
  reg  outputMemBlock_12;
  reg [31:0] _RAND_46;
  reg  outputMemBlock_13;
  reg [31:0] _RAND_47;
  reg  outputMemBlock_14;
  reg [31:0] _RAND_48;
  reg  outputMemBlock_15;
  reg [31:0] _RAND_49;
  reg  outputMemBlock_16;
  reg [31:0] _RAND_50;
  reg  outputMemBlock_17;
  reg [31:0] _RAND_51;
  reg  outputMemBlock_18;
  reg [31:0] _RAND_52;
  reg  outputMemBlock_19;
  reg [31:0] _RAND_53;
  reg  outputMemBlock_20;
  reg [31:0] _RAND_54;
  reg  outputMemBlock_21;
  reg [31:0] _RAND_55;
  reg  outputMemBlock_22;
  reg [31:0] _RAND_56;
  reg  outputMemBlock_23;
  reg [31:0] _RAND_57;
  reg  outputMemBlock_24;
  reg [31:0] _RAND_58;
  reg  outputMemBlock_25;
  reg [31:0] _RAND_59;
  reg  outputMemBlock_26;
  reg [31:0] _RAND_60;
  reg  outputMemBlock_27;
  reg [31:0] _RAND_61;
  reg  outputMemBlock_28;
  reg [31:0] _RAND_62;
  reg  outputMemBlock_29;
  reg [31:0] _RAND_63;
  reg  outputMemBlock_30;
  reg [31:0] _RAND_64;
  reg  outputMemBlock_31;
  reg [31:0] _RAND_65;
  reg [10:0] outputBits;
  reg [31:0] _RAND_66;
  reg [5:0] outputPieceBits;
  reg [31:0] _RAND_67;
  wire  inputBram_clock;
  wire [4:0] inputBram_io_a_addr;
  wire [31:0] inputBram_io_a_din;
  wire  inputBram_io_a_wr;
  wire [4:0] inputBram_io_b_addr;
  wire [31:0] inputBram_io_b_dout;
  reg [4:0] inputReadAddr;
  reg [31:0] _RAND_68;
  wire  outputBram_clock;
  wire [4:0] outputBram_io_a_addr;
  wire [31:0] outputBram_io_a_din;
  wire  outputBram_io_a_wr;
  wire [4:0] outputBram_io_b_addr;
  wire [31:0] outputBram_io_b_dout;
  reg [4:0] outputWriteAddr;
  reg [31:0] _RAND_69;
  reg [4:0] outputReadAddr;
  reg [31:0] _RAND_70;
  wire  _T_102;
  wire  _T_103;
  wire [10:0] _GEN_0;
  wire [4:0] inputReadAddrFinal;
  wire  _T_107;
  wire  _T_109;
  wire  _T_111;
  wire  _T_112;
  wire  _T_113;
  wire [6:0] _T_115;
  wire [6:0] _T_116;
  wire [5:0] _T_117;
  wire  _T_119;
  wire  _T_120;
  wire  _T_121;
  wire [11:0] _T_125;
  wire [11:0] _T_126;
  wire [10:0] _T_127;
  wire [10:0] _T_128;
  wire  _T_130;
  wire [10:0] _T_132;
  wire  _T_134;
  wire [5:0] _T_136;
  wire [4:0] _T_137;
  wire [4:0] _T_138;
  wire  _T_142;
  wire  _T_143;
  wire  _T_144;
  wire  _T_145;
  wire  _T_146;
  wire  _T_147;
  wire  _T_148;
  wire  _T_149;
  wire  _T_150;
  wire  _T_151;
  wire  _T_152;
  wire  _T_153;
  wire  _T_154;
  wire  _T_155;
  wire  _T_156;
  wire  _T_157;
  wire  _T_158;
  wire  _T_159;
  wire  _T_160;
  wire  _T_161;
  wire  _T_162;
  wire  _T_163;
  wire  _T_164;
  wire  _T_165;
  wire  _T_166;
  wire  _T_167;
  wire  _T_168;
  wire  _T_169;
  wire  _T_170;
  wire  _T_171;
  wire  _T_172;
  wire  _T_173;
  wire [10:0] _GEN_1;
  wire [10:0] _GEN_2;
  wire [4:0] _GEN_3;
  wire [4:0] _GEN_4;
  wire  _GEN_5;
  wire  _GEN_6;
  wire  _GEN_7;
  wire  _GEN_8;
  wire  _GEN_9;
  wire  _GEN_10;
  wire  _GEN_11;
  wire  _GEN_12;
  wire  _GEN_13;
  wire  _GEN_14;
  wire  _GEN_15;
  wire  _GEN_16;
  wire  _GEN_17;
  wire  _GEN_18;
  wire  _GEN_19;
  wire  _GEN_20;
  wire  _GEN_21;
  wire  _GEN_22;
  wire  _GEN_23;
  wire  _GEN_24;
  wire  _GEN_25;
  wire  _GEN_26;
  wire  _GEN_27;
  wire  _GEN_28;
  wire  _GEN_29;
  wire  _GEN_30;
  wire  _GEN_31;
  wire  _GEN_32;
  wire  _GEN_33;
  wire  _GEN_34;
  wire  _GEN_35;
  wire  _GEN_36;
  wire  _T_182;
  wire  _T_183;
  wire [10:0] _GEN_37;
  wire [10:0] _GEN_38;
  wire [1:0] _T_192;
  wire [1:0] _T_193;
  wire [3:0] _T_194;
  wire [1:0] _T_195;
  wire [1:0] _T_196;
  wire [3:0] _T_197;
  wire [7:0] _T_198;
  wire [1:0] _T_199;
  wire [1:0] _T_200;
  wire [3:0] _T_201;
  wire [1:0] _T_202;
  wire [1:0] _T_203;
  wire [3:0] _T_204;
  wire [7:0] _T_205;
  wire [15:0] _T_206;
  wire [1:0] _T_207;
  wire [1:0] _T_208;
  wire [3:0] _T_209;
  wire [1:0] _T_210;
  wire [1:0] _T_211;
  wire [3:0] _T_212;
  wire [7:0] _T_213;
  wire [1:0] _T_214;
  wire [1:0] _T_215;
  wire [3:0] _T_216;
  wire [1:0] _T_217;
  wire [1:0] _T_218;
  wire [3:0] _T_219;
  wire [7:0] _T_220;
  wire [15:0] _T_221;
  wire [31:0] nextWord;
  wire  _T_226;
  wire  _T_229;
  wire  _T_231;
  wire  _T_233;
  wire  _T_234;
  wire  _T_236;
  wire  _T_237;
  wire  _T_239;
  wire  _T_240;
  wire  _T_241;
  wire  _T_242;
  wire  _T_243;
  wire  _T_244;
  wire  _T_245;
  wire  _T_246;
  wire  _T_247;
  wire  _T_248;
  wire  _T_249;
  wire  _T_250;
  wire  _T_251;
  wire  _T_252;
  wire  _T_253;
  wire  _T_254;
  wire  _T_255;
  wire  _T_256;
  wire  _T_257;
  wire  _T_258;
  wire  _T_259;
  wire  _T_260;
  wire  _T_261;
  wire  _T_262;
  wire  _T_263;
  wire  _T_264;
  wire  _T_265;
  wire  _T_266;
  wire  _T_267;
  wire  _T_268;
  wire  _T_269;
  wire  _T_270;
  wire  _T_271;
  wire  _T_272;
  wire  _T_273;
  wire  _T_275;
  wire [6:0] _T_278;
  wire [5:0] _T_279;
  wire [5:0] _T_280;
  wire [5:0] _T_284;
  wire [4:0] _T_285;
  wire [4:0] _T_286;
  wire [11:0] _T_288;
  wire [10:0] _T_289;
  wire [10:0] _GEN_39;
  wire  _GEN_40;
  wire  _GEN_41;
  wire  _GEN_42;
  wire  _GEN_43;
  wire  _GEN_44;
  wire  _GEN_45;
  wire  _GEN_46;
  wire  _GEN_47;
  wire  _GEN_48;
  wire  _GEN_49;
  wire  _GEN_50;
  wire  _GEN_51;
  wire  _GEN_52;
  wire  _GEN_53;
  wire  _GEN_54;
  wire  _GEN_55;
  wire  _GEN_56;
  wire  _GEN_57;
  wire  _GEN_58;
  wire  _GEN_59;
  wire  _GEN_60;
  wire  _GEN_61;
  wire  _GEN_62;
  wire  _GEN_63;
  wire  _GEN_64;
  wire  _GEN_65;
  wire  _GEN_66;
  wire  _GEN_67;
  wire  _GEN_68;
  wire  _GEN_69;
  wire  _GEN_70;
  wire  _GEN_71;
  wire [5:0] _GEN_72;
  wire [4:0] _GEN_73;
  wire [10:0] _GEN_74;
  wire [1:0] _T_292;
  wire [1:0] _T_293;
  wire [3:0] _T_294;
  wire [1:0] _T_295;
  wire [1:0] _T_296;
  wire [3:0] _T_297;
  wire [7:0] _T_298;
  wire [1:0] _T_299;
  wire [1:0] _T_300;
  wire [3:0] _T_301;
  wire [1:0] _T_302;
  wire [1:0] _T_303;
  wire [3:0] _T_304;
  wire [7:0] _T_305;
  wire [15:0] _T_306;
  wire [1:0] _T_307;
  wire [1:0] _T_308;
  wire [3:0] _T_309;
  wire [1:0] _T_310;
  wire [1:0] _T_311;
  wire [3:0] _T_312;
  wire [7:0] _T_313;
  wire [1:0] _T_314;
  wire [1:0] _T_315;
  wire [3:0] _T_316;
  wire [1:0] _T_317;
  wire [1:0] _T_318;
  wire [3:0] _T_319;
  wire [7:0] _T_320;
  wire [15:0] _T_321;
  wire [31:0] _T_322;
  reg  outputReadingStartedPrev;
  reg [31:0] _RAND_71;
  reg  outputReadingStarted;
  reg [31:0] _RAND_72;
  wire  _T_329;
  wire  _T_333;
  wire  _T_334;
  wire  _T_337;
  wire  _T_338;
  wire  _T_339;
  wire  _GEN_75;
  wire  _T_342;
  wire  _T_343;
  wire  _GEN_76;
  wire  _T_345;
  wire  _T_347;
  wire  _T_349;
  wire  _T_350;
  wire [5:0] _T_352;
  wire [4:0] _T_353;
  wire  _T_355;
  wire [10:0] _GEN_77;
  wire [4:0] _GEN_78;
  wire [10:0] _GEN_79;
  wire  _T_360;
  wire  _T_361;
  wire  _T_364;
  wire  _T_367;
  wire  _GEN_80;
  wire  _GEN_81;
  wire [4:0] _GEN_82;
  PassThrough inner (
    .clock(inner_clock),
    .reset(inner_reset),
    .io_inputWord(inner_io_inputWord),
    .io_inputValid(inner_io_inputValid),
    .io_inputFinished(inner_io_inputFinished),
    .io_inputReady(inner_io_inputReady),
    .io_outputWord(inner_io_outputWord),
    .io_outputValid(inner_io_outputValid),
    .io_outputFinished(inner_io_outputFinished),
    .io_outputReady(inner_io_outputReady)
  );
  DualPortBRAM inputBram (
    .clock(inputBram_clock),
    .io_a_addr(inputBram_io_a_addr),
    .io_a_din(inputBram_io_a_din),
    .io_a_wr(inputBram_io_a_wr),
    .io_b_addr(inputBram_io_b_addr),
    .io_b_dout(inputBram_io_b_dout)
  );
  DualPortBRAM outputBram (
    .clock(outputBram_clock),
    .io_a_addr(outputBram_io_a_addr),
    .io_a_din(outputBram_io_a_din),
    .io_a_wr(outputBram_io_a_wr),
    .io_b_addr(outputBram_io_b_addr),
    .io_b_dout(outputBram_io_b_dout)
  );
  assign _T_102 = io_inputMemIdx == 5'h1;
  assign _T_103 = io_inputMemBlockValid & _T_102;
  assign _GEN_0 = _T_103 ? io_inputBits : inputBitsRemaining;
  assign _T_107 = inputPieceBitsRemaining == 6'h0;
  assign _T_109 = inputBitsRemaining == 11'h0;
  assign _T_111 = _T_109 == 1'h0;
  assign _T_112 = _T_107 & _T_111;
  assign _T_113 = inner_io_inputValid & inner_io_inputReady;
  assign _T_115 = inputPieceBitsRemaining - 6'h20;
  assign _T_116 = $unsigned(_T_115);
  assign _T_117 = _T_116[5:0];
  assign _T_119 = _T_117 == 6'h0;
  assign _T_120 = _T_113 & _T_119;
  assign _T_121 = _T_112 | _T_120;
  assign _T_125 = inputBitsRemaining - 11'h20;
  assign _T_126 = $unsigned(_T_125);
  assign _T_127 = _T_126[10:0];
  assign _T_128 = _T_107 ? inputBitsRemaining : _T_127;
  assign _T_130 = _T_128 < 11'h20;
  assign _T_132 = _T_130 ? _T_128 : 11'h20;
  assign _T_134 = _T_128 == 11'h0;
  assign _T_136 = inputReadAddr + 5'h1;
  assign _T_137 = _T_136[4:0];
  assign _T_138 = _T_134 ? inputReadAddr : _T_137;
  assign _T_142 = inputBram_io_b_dout[0];
  assign _T_143 = inputBram_io_b_dout[1];
  assign _T_144 = inputBram_io_b_dout[2];
  assign _T_145 = inputBram_io_b_dout[3];
  assign _T_146 = inputBram_io_b_dout[4];
  assign _T_147 = inputBram_io_b_dout[5];
  assign _T_148 = inputBram_io_b_dout[6];
  assign _T_149 = inputBram_io_b_dout[7];
  assign _T_150 = inputBram_io_b_dout[8];
  assign _T_151 = inputBram_io_b_dout[9];
  assign _T_152 = inputBram_io_b_dout[10];
  assign _T_153 = inputBram_io_b_dout[11];
  assign _T_154 = inputBram_io_b_dout[12];
  assign _T_155 = inputBram_io_b_dout[13];
  assign _T_156 = inputBram_io_b_dout[14];
  assign _T_157 = inputBram_io_b_dout[15];
  assign _T_158 = inputBram_io_b_dout[16];
  assign _T_159 = inputBram_io_b_dout[17];
  assign _T_160 = inputBram_io_b_dout[18];
  assign _T_161 = inputBram_io_b_dout[19];
  assign _T_162 = inputBram_io_b_dout[20];
  assign _T_163 = inputBram_io_b_dout[21];
  assign _T_164 = inputBram_io_b_dout[22];
  assign _T_165 = inputBram_io_b_dout[23];
  assign _T_166 = inputBram_io_b_dout[24];
  assign _T_167 = inputBram_io_b_dout[25];
  assign _T_168 = inputBram_io_b_dout[26];
  assign _T_169 = inputBram_io_b_dout[27];
  assign _T_170 = inputBram_io_b_dout[28];
  assign _T_171 = inputBram_io_b_dout[29];
  assign _T_172 = inputBram_io_b_dout[30];
  assign _T_173 = inputBram_io_b_dout[31];
  assign _GEN_1 = _T_121 ? _T_132 : {{5'd0}, inputPieceBitsRemaining};
  assign _GEN_2 = _T_121 ? _T_128 : _GEN_0;
  assign _GEN_3 = _T_121 ? _T_138 : inputReadAddr;
  assign _GEN_4 = _T_121 ? _T_137 : inputReadAddr;
  assign _GEN_5 = _T_121 ? _T_142 : inputMemBlock_0;
  assign _GEN_6 = _T_121 ? _T_143 : inputMemBlock_1;
  assign _GEN_7 = _T_121 ? _T_144 : inputMemBlock_2;
  assign _GEN_8 = _T_121 ? _T_145 : inputMemBlock_3;
  assign _GEN_9 = _T_121 ? _T_146 : inputMemBlock_4;
  assign _GEN_10 = _T_121 ? _T_147 : inputMemBlock_5;
  assign _GEN_11 = _T_121 ? _T_148 : inputMemBlock_6;
  assign _GEN_12 = _T_121 ? _T_149 : inputMemBlock_7;
  assign _GEN_13 = _T_121 ? _T_150 : inputMemBlock_8;
  assign _GEN_14 = _T_121 ? _T_151 : inputMemBlock_9;
  assign _GEN_15 = _T_121 ? _T_152 : inputMemBlock_10;
  assign _GEN_16 = _T_121 ? _T_153 : inputMemBlock_11;
  assign _GEN_17 = _T_121 ? _T_154 : inputMemBlock_12;
  assign _GEN_18 = _T_121 ? _T_155 : inputMemBlock_13;
  assign _GEN_19 = _T_121 ? _T_156 : inputMemBlock_14;
  assign _GEN_20 = _T_121 ? _T_157 : inputMemBlock_15;
  assign _GEN_21 = _T_121 ? _T_158 : inputMemBlock_16;
  assign _GEN_22 = _T_121 ? _T_159 : inputMemBlock_17;
  assign _GEN_23 = _T_121 ? _T_160 : inputMemBlock_18;
  assign _GEN_24 = _T_121 ? _T_161 : inputMemBlock_19;
  assign _GEN_25 = _T_121 ? _T_162 : inputMemBlock_20;
  assign _GEN_26 = _T_121 ? _T_163 : inputMemBlock_21;
  assign _GEN_27 = _T_121 ? _T_164 : inputMemBlock_22;
  assign _GEN_28 = _T_121 ? _T_165 : inputMemBlock_23;
  assign _GEN_29 = _T_121 ? _T_166 : inputMemBlock_24;
  assign _GEN_30 = _T_121 ? _T_167 : inputMemBlock_25;
  assign _GEN_31 = _T_121 ? _T_168 : inputMemBlock_26;
  assign _GEN_32 = _T_121 ? _T_169 : inputMemBlock_27;
  assign _GEN_33 = _T_121 ? _T_170 : inputMemBlock_28;
  assign _GEN_34 = _T_121 ? _T_171 : inputMemBlock_29;
  assign _GEN_35 = _T_121 ? _T_172 : inputMemBlock_30;
  assign _GEN_36 = _T_121 ? _T_173 : inputMemBlock_31;
  assign _T_182 = _T_119 == 1'h0;
  assign _T_183 = _T_113 & _T_182;
  assign _GEN_37 = _T_183 ? {{5'd0}, _T_117} : _GEN_1;
  assign _GEN_38 = _T_183 ? _T_127 : _GEN_2;
  assign _T_192 = {inputMemBlock_1,inputMemBlock_0};
  assign _T_193 = {inputMemBlock_3,inputMemBlock_2};
  assign _T_194 = {_T_193,_T_192};
  assign _T_195 = {inputMemBlock_5,inputMemBlock_4};
  assign _T_196 = {inputMemBlock_7,inputMemBlock_6};
  assign _T_197 = {_T_196,_T_195};
  assign _T_198 = {_T_197,_T_194};
  assign _T_199 = {inputMemBlock_9,inputMemBlock_8};
  assign _T_200 = {inputMemBlock_11,inputMemBlock_10};
  assign _T_201 = {_T_200,_T_199};
  assign _T_202 = {inputMemBlock_13,inputMemBlock_12};
  assign _T_203 = {inputMemBlock_15,inputMemBlock_14};
  assign _T_204 = {_T_203,_T_202};
  assign _T_205 = {_T_204,_T_201};
  assign _T_206 = {_T_205,_T_198};
  assign _T_207 = {inputMemBlock_17,inputMemBlock_16};
  assign _T_208 = {inputMemBlock_19,inputMemBlock_18};
  assign _T_209 = {_T_208,_T_207};
  assign _T_210 = {inputMemBlock_21,inputMemBlock_20};
  assign _T_211 = {inputMemBlock_23,inputMemBlock_22};
  assign _T_212 = {_T_211,_T_210};
  assign _T_213 = {_T_212,_T_209};
  assign _T_214 = {inputMemBlock_25,inputMemBlock_24};
  assign _T_215 = {inputMemBlock_27,inputMemBlock_26};
  assign _T_216 = {_T_215,_T_214};
  assign _T_217 = {inputMemBlock_29,inputMemBlock_28};
  assign _T_218 = {inputMemBlock_31,inputMemBlock_30};
  assign _T_219 = {_T_218,_T_217};
  assign _T_220 = {_T_219,_T_216};
  assign _T_221 = {_T_220,_T_213};
  assign nextWord = {_T_221,_T_206};
  assign _T_226 = _T_107 == 1'h0;
  assign _T_229 = io_inputFinished & _T_109;
  assign _T_231 = outputBits == 11'h400;
  assign _T_233 = _T_231 == 1'h0;
  assign _T_234 = inner_io_outputValid & inner_io_outputReady;
  assign _T_236 = outputPieceBits > 6'h0;
  assign _T_237 = inner_io_outputFinished & _T_236;
  assign _T_239 = outputPieceBits < 6'h20;
  assign _T_240 = _T_237 & _T_239;
  assign _T_241 = _T_234 | _T_240;
  assign _T_242 = inner_io_outputWord[31];
  assign _T_243 = inner_io_outputWord[30];
  assign _T_244 = inner_io_outputWord[29];
  assign _T_245 = inner_io_outputWord[28];
  assign _T_246 = inner_io_outputWord[27];
  assign _T_247 = inner_io_outputWord[26];
  assign _T_248 = inner_io_outputWord[25];
  assign _T_249 = inner_io_outputWord[24];
  assign _T_250 = inner_io_outputWord[23];
  assign _T_251 = inner_io_outputWord[22];
  assign _T_252 = inner_io_outputWord[21];
  assign _T_253 = inner_io_outputWord[20];
  assign _T_254 = inner_io_outputWord[19];
  assign _T_255 = inner_io_outputWord[18];
  assign _T_256 = inner_io_outputWord[17];
  assign _T_257 = inner_io_outputWord[16];
  assign _T_258 = inner_io_outputWord[15];
  assign _T_259 = inner_io_outputWord[14];
  assign _T_260 = inner_io_outputWord[13];
  assign _T_261 = inner_io_outputWord[12];
  assign _T_262 = inner_io_outputWord[11];
  assign _T_263 = inner_io_outputWord[10];
  assign _T_264 = inner_io_outputWord[9];
  assign _T_265 = inner_io_outputWord[8];
  assign _T_266 = inner_io_outputWord[7];
  assign _T_267 = inner_io_outputWord[6];
  assign _T_268 = inner_io_outputWord[5];
  assign _T_269 = inner_io_outputWord[4];
  assign _T_270 = inner_io_outputWord[3];
  assign _T_271 = inner_io_outputWord[2];
  assign _T_272 = inner_io_outputWord[1];
  assign _T_273 = inner_io_outputWord[0];
  assign _T_275 = outputPieceBits == 6'h20;
  assign _T_278 = outputPieceBits + 6'h20;
  assign _T_279 = _T_278[5:0];
  assign _T_280 = _T_275 ? 6'h20 : _T_279;
  assign _T_284 = outputWriteAddr + 5'h1;
  assign _T_285 = _T_284[4:0];
  assign _T_286 = _T_275 ? _T_285 : outputWriteAddr;
  assign _T_288 = outputBits + 11'h20;
  assign _T_289 = _T_288[10:0];
  assign _GEN_39 = inner_io_outputValid ? _T_289 : outputBits;
  assign _GEN_40 = _T_241 ? _T_242 : outputMemBlock_31;
  assign _GEN_41 = _T_241 ? _T_243 : outputMemBlock_30;
  assign _GEN_42 = _T_241 ? _T_244 : outputMemBlock_29;
  assign _GEN_43 = _T_241 ? _T_245 : outputMemBlock_28;
  assign _GEN_44 = _T_241 ? _T_246 : outputMemBlock_27;
  assign _GEN_45 = _T_241 ? _T_247 : outputMemBlock_26;
  assign _GEN_46 = _T_241 ? _T_248 : outputMemBlock_25;
  assign _GEN_47 = _T_241 ? _T_249 : outputMemBlock_24;
  assign _GEN_48 = _T_241 ? _T_250 : outputMemBlock_23;
  assign _GEN_49 = _T_241 ? _T_251 : outputMemBlock_22;
  assign _GEN_50 = _T_241 ? _T_252 : outputMemBlock_21;
  assign _GEN_51 = _T_241 ? _T_253 : outputMemBlock_20;
  assign _GEN_52 = _T_241 ? _T_254 : outputMemBlock_19;
  assign _GEN_53 = _T_241 ? _T_255 : outputMemBlock_18;
  assign _GEN_54 = _T_241 ? _T_256 : outputMemBlock_17;
  assign _GEN_55 = _T_241 ? _T_257 : outputMemBlock_16;
  assign _GEN_56 = _T_241 ? _T_258 : outputMemBlock_15;
  assign _GEN_57 = _T_241 ? _T_259 : outputMemBlock_14;
  assign _GEN_58 = _T_241 ? _T_260 : outputMemBlock_13;
  assign _GEN_59 = _T_241 ? _T_261 : outputMemBlock_12;
  assign _GEN_60 = _T_241 ? _T_262 : outputMemBlock_11;
  assign _GEN_61 = _T_241 ? _T_263 : outputMemBlock_10;
  assign _GEN_62 = _T_241 ? _T_264 : outputMemBlock_9;
  assign _GEN_63 = _T_241 ? _T_265 : outputMemBlock_8;
  assign _GEN_64 = _T_241 ? _T_266 : outputMemBlock_7;
  assign _GEN_65 = _T_241 ? _T_267 : outputMemBlock_6;
  assign _GEN_66 = _T_241 ? _T_268 : outputMemBlock_5;
  assign _GEN_67 = _T_241 ? _T_269 : outputMemBlock_4;
  assign _GEN_68 = _T_241 ? _T_270 : outputMemBlock_3;
  assign _GEN_69 = _T_241 ? _T_271 : outputMemBlock_2;
  assign _GEN_70 = _T_241 ? _T_272 : outputMemBlock_1;
  assign _GEN_71 = _T_241 ? _T_273 : outputMemBlock_0;
  assign _GEN_72 = _T_241 ? _T_280 : outputPieceBits;
  assign _GEN_73 = _T_241 ? _T_286 : outputWriteAddr;
  assign _GEN_74 = _T_241 ? _GEN_39 : outputBits;
  assign _T_292 = {outputMemBlock_1,outputMemBlock_0};
  assign _T_293 = {outputMemBlock_3,outputMemBlock_2};
  assign _T_294 = {_T_293,_T_292};
  assign _T_295 = {outputMemBlock_5,outputMemBlock_4};
  assign _T_296 = {outputMemBlock_7,outputMemBlock_6};
  assign _T_297 = {_T_296,_T_295};
  assign _T_298 = {_T_297,_T_294};
  assign _T_299 = {outputMemBlock_9,outputMemBlock_8};
  assign _T_300 = {outputMemBlock_11,outputMemBlock_10};
  assign _T_301 = {_T_300,_T_299};
  assign _T_302 = {outputMemBlock_13,outputMemBlock_12};
  assign _T_303 = {outputMemBlock_15,outputMemBlock_14};
  assign _T_304 = {_T_303,_T_302};
  assign _T_305 = {_T_304,_T_301};
  assign _T_306 = {_T_305,_T_298};
  assign _T_307 = {outputMemBlock_17,outputMemBlock_16};
  assign _T_308 = {outputMemBlock_19,outputMemBlock_18};
  assign _T_309 = {_T_308,_T_307};
  assign _T_310 = {outputMemBlock_21,outputMemBlock_20};
  assign _T_311 = {outputMemBlock_23,outputMemBlock_22};
  assign _T_312 = {_T_311,_T_310};
  assign _T_313 = {_T_312,_T_309};
  assign _T_314 = {outputMemBlock_25,outputMemBlock_24};
  assign _T_315 = {outputMemBlock_27,outputMemBlock_26};
  assign _T_316 = {_T_315,_T_314};
  assign _T_317 = {outputMemBlock_29,outputMemBlock_28};
  assign _T_318 = {outputMemBlock_31,outputMemBlock_30};
  assign _T_319 = {_T_318,_T_317};
  assign _T_320 = {_T_319,_T_316};
  assign _T_321 = {_T_320,_T_313};
  assign _T_322 = {_T_321,_T_306};
  assign _T_329 = outputReadingStartedPrev == 1'h0;
  assign _T_333 = outputBits > 11'h0;
  assign _T_334 = inner_io_outputFinished & _T_333;
  assign _T_337 = _T_334 & _T_275;
  assign _T_338 = _T_231 | _T_337;
  assign _T_339 = _T_329 & _T_338;
  assign _GEN_75 = _T_339 ? 1'h1 : outputReadingStartedPrev;
  assign _T_342 = outputReadingStarted == 1'h0;
  assign _T_343 = outputReadingStartedPrev & _T_342;
  assign _GEN_76 = _T_343 ? 1'h1 : outputReadingStarted;
  assign _T_345 = io_outputMemBlockReady & outputReadingStarted;
  assign _T_347 = outputReadAddr == 5'h1f;
  assign _T_349 = _T_347 == 1'h0;
  assign _T_350 = _T_345 & _T_349;
  assign _T_352 = outputReadAddr + 5'h1;
  assign _T_353 = _T_352[4:0];
  assign _T_355 = outputReadAddr == 5'h0;
  assign _GEN_77 = _T_355 ? 11'h0 : _GEN_74;
  assign _GEN_78 = _T_350 ? _T_353 : outputReadAddr;
  assign _GEN_79 = _T_350 ? _GEN_77 : _GEN_74;
  assign _T_360 = outputBits == 11'h0;
  assign _T_361 = inner_io_outputFinished & _T_360;
  assign _T_364 = _T_361 & _T_355;
  assign _T_367 = outputReadingStarted & _T_347;
  assign _GEN_80 = _T_367 ? 1'h0 : _GEN_75;
  assign _GEN_81 = _T_367 ? 1'h0 : _GEN_76;
  assign _GEN_82 = _T_367 ? 5'h0 : _GEN_78;
  assign io_inputMemConsumed = _T_109;
  assign io_outputMemBlock = outputBram_io_b_dout;
  assign io_outputMemBlockValid = outputReadingStarted;
  assign io_outputBits = outputBits;
  assign io_outputFinished = _T_364;
  assign inner_io_inputWord = nextWord;
  assign inner_io_inputValid = _T_226;
  assign inner_io_inputFinished = _T_229;
  assign inner_io_outputReady = _T_233;
  assign inner_clock = clock;
  assign inner_reset = reset;
  assign inputBram_io_a_addr = io_inputMemIdx;
  assign inputBram_io_a_din = io_inputMemBlock;
  assign inputBram_io_a_wr = io_inputMemBlockValid;
  assign inputBram_io_b_addr = inputReadAddrFinal;
  assign inputBram_clock = clock;
  assign outputBram_io_a_addr = outputWriteAddr;
  assign outputBram_io_a_din = _T_322;
  assign outputBram_io_a_wr = _T_275;
  assign outputBram_io_b_addr = outputReadAddr;
  assign outputBram_clock = clock;
  assign inputReadAddrFinal = _GEN_4;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{$random}};
  inputMemBlock_0 = _RAND_0[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{$random}};
  inputMemBlock_1 = _RAND_1[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{$random}};
  inputMemBlock_2 = _RAND_2[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{$random}};
  inputMemBlock_3 = _RAND_3[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_4 = {1{$random}};
  inputMemBlock_4 = _RAND_4[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_5 = {1{$random}};
  inputMemBlock_5 = _RAND_5[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_6 = {1{$random}};
  inputMemBlock_6 = _RAND_6[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_7 = {1{$random}};
  inputMemBlock_7 = _RAND_7[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_8 = {1{$random}};
  inputMemBlock_8 = _RAND_8[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_9 = {1{$random}};
  inputMemBlock_9 = _RAND_9[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_10 = {1{$random}};
  inputMemBlock_10 = _RAND_10[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_11 = {1{$random}};
  inputMemBlock_11 = _RAND_11[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_12 = {1{$random}};
  inputMemBlock_12 = _RAND_12[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_13 = {1{$random}};
  inputMemBlock_13 = _RAND_13[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_14 = {1{$random}};
  inputMemBlock_14 = _RAND_14[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_15 = {1{$random}};
  inputMemBlock_15 = _RAND_15[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_16 = {1{$random}};
  inputMemBlock_16 = _RAND_16[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_17 = {1{$random}};
  inputMemBlock_17 = _RAND_17[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_18 = {1{$random}};
  inputMemBlock_18 = _RAND_18[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_19 = {1{$random}};
  inputMemBlock_19 = _RAND_19[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_20 = {1{$random}};
  inputMemBlock_20 = _RAND_20[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_21 = {1{$random}};
  inputMemBlock_21 = _RAND_21[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_22 = {1{$random}};
  inputMemBlock_22 = _RAND_22[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_23 = {1{$random}};
  inputMemBlock_23 = _RAND_23[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_24 = {1{$random}};
  inputMemBlock_24 = _RAND_24[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_25 = {1{$random}};
  inputMemBlock_25 = _RAND_25[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_26 = {1{$random}};
  inputMemBlock_26 = _RAND_26[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_27 = {1{$random}};
  inputMemBlock_27 = _RAND_27[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_28 = {1{$random}};
  inputMemBlock_28 = _RAND_28[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_29 = {1{$random}};
  inputMemBlock_29 = _RAND_29[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_30 = {1{$random}};
  inputMemBlock_30 = _RAND_30[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_31 = {1{$random}};
  inputMemBlock_31 = _RAND_31[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_32 = {1{$random}};
  inputPieceBitsRemaining = _RAND_32[5:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_33 = {1{$random}};
  inputBitsRemaining = _RAND_33[10:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_34 = {1{$random}};
  outputMemBlock_0 = _RAND_34[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_35 = {1{$random}};
  outputMemBlock_1 = _RAND_35[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_36 = {1{$random}};
  outputMemBlock_2 = _RAND_36[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_37 = {1{$random}};
  outputMemBlock_3 = _RAND_37[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_38 = {1{$random}};
  outputMemBlock_4 = _RAND_38[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_39 = {1{$random}};
  outputMemBlock_5 = _RAND_39[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_40 = {1{$random}};
  outputMemBlock_6 = _RAND_40[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_41 = {1{$random}};
  outputMemBlock_7 = _RAND_41[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_42 = {1{$random}};
  outputMemBlock_8 = _RAND_42[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_43 = {1{$random}};
  outputMemBlock_9 = _RAND_43[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_44 = {1{$random}};
  outputMemBlock_10 = _RAND_44[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_45 = {1{$random}};
  outputMemBlock_11 = _RAND_45[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_46 = {1{$random}};
  outputMemBlock_12 = _RAND_46[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_47 = {1{$random}};
  outputMemBlock_13 = _RAND_47[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_48 = {1{$random}};
  outputMemBlock_14 = _RAND_48[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_49 = {1{$random}};
  outputMemBlock_15 = _RAND_49[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_50 = {1{$random}};
  outputMemBlock_16 = _RAND_50[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_51 = {1{$random}};
  outputMemBlock_17 = _RAND_51[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_52 = {1{$random}};
  outputMemBlock_18 = _RAND_52[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_53 = {1{$random}};
  outputMemBlock_19 = _RAND_53[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_54 = {1{$random}};
  outputMemBlock_20 = _RAND_54[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_55 = {1{$random}};
  outputMemBlock_21 = _RAND_55[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_56 = {1{$random}};
  outputMemBlock_22 = _RAND_56[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_57 = {1{$random}};
  outputMemBlock_23 = _RAND_57[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_58 = {1{$random}};
  outputMemBlock_24 = _RAND_58[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_59 = {1{$random}};
  outputMemBlock_25 = _RAND_59[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_60 = {1{$random}};
  outputMemBlock_26 = _RAND_60[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_61 = {1{$random}};
  outputMemBlock_27 = _RAND_61[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_62 = {1{$random}};
  outputMemBlock_28 = _RAND_62[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_63 = {1{$random}};
  outputMemBlock_29 = _RAND_63[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_64 = {1{$random}};
  outputMemBlock_30 = _RAND_64[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_65 = {1{$random}};
  outputMemBlock_31 = _RAND_65[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_66 = {1{$random}};
  outputBits = _RAND_66[10:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_67 = {1{$random}};
  outputPieceBits = _RAND_67[5:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_68 = {1{$random}};
  inputReadAddr = _RAND_68[4:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_69 = {1{$random}};
  outputWriteAddr = _RAND_69[4:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_70 = {1{$random}};
  outputReadAddr = _RAND_70[4:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_71 = {1{$random}};
  outputReadingStartedPrev = _RAND_71[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_72 = {1{$random}};
  outputReadingStarted = _RAND_72[0:0];
  `endif // RANDOMIZE_REG_INIT
  end
`endif // RANDOMIZE
  always @(posedge clock) begin
    if (_T_121) begin
      inputMemBlock_0 <= _T_142;
    end
    if (_T_121) begin
      inputMemBlock_1 <= _T_143;
    end
    if (_T_121) begin
      inputMemBlock_2 <= _T_144;
    end
    if (_T_121) begin
      inputMemBlock_3 <= _T_145;
    end
    if (_T_121) begin
      inputMemBlock_4 <= _T_146;
    end
    if (_T_121) begin
      inputMemBlock_5 <= _T_147;
    end
    if (_T_121) begin
      inputMemBlock_6 <= _T_148;
    end
    if (_T_121) begin
      inputMemBlock_7 <= _T_149;
    end
    if (_T_121) begin
      inputMemBlock_8 <= _T_150;
    end
    if (_T_121) begin
      inputMemBlock_9 <= _T_151;
    end
    if (_T_121) begin
      inputMemBlock_10 <= _T_152;
    end
    if (_T_121) begin
      inputMemBlock_11 <= _T_153;
    end
    if (_T_121) begin
      inputMemBlock_12 <= _T_154;
    end
    if (_T_121) begin
      inputMemBlock_13 <= _T_155;
    end
    if (_T_121) begin
      inputMemBlock_14 <= _T_156;
    end
    if (_T_121) begin
      inputMemBlock_15 <= _T_157;
    end
    if (_T_121) begin
      inputMemBlock_16 <= _T_158;
    end
    if (_T_121) begin
      inputMemBlock_17 <= _T_159;
    end
    if (_T_121) begin
      inputMemBlock_18 <= _T_160;
    end
    if (_T_121) begin
      inputMemBlock_19 <= _T_161;
    end
    if (_T_121) begin
      inputMemBlock_20 <= _T_162;
    end
    if (_T_121) begin
      inputMemBlock_21 <= _T_163;
    end
    if (_T_121) begin
      inputMemBlock_22 <= _T_164;
    end
    if (_T_121) begin
      inputMemBlock_23 <= _T_165;
    end
    if (_T_121) begin
      inputMemBlock_24 <= _T_166;
    end
    if (_T_121) begin
      inputMemBlock_25 <= _T_167;
    end
    if (_T_121) begin
      inputMemBlock_26 <= _T_168;
    end
    if (_T_121) begin
      inputMemBlock_27 <= _T_169;
    end
    if (_T_121) begin
      inputMemBlock_28 <= _T_170;
    end
    if (_T_121) begin
      inputMemBlock_29 <= _T_171;
    end
    if (_T_121) begin
      inputMemBlock_30 <= _T_172;
    end
    if (_T_121) begin
      inputMemBlock_31 <= _T_173;
    end
    if (reset) begin
      inputPieceBitsRemaining <= 6'h0;
    end else begin
      inputPieceBitsRemaining <= _GEN_37[5:0];
    end
    if (reset) begin
      inputBitsRemaining <= 11'h0;
    end else begin
      if (_T_183) begin
        inputBitsRemaining <= _T_127;
      end else begin
        if (_T_121) begin
          if (!(_T_107)) begin
            inputBitsRemaining <= _T_127;
          end
        end else begin
          if (_T_103) begin
            inputBitsRemaining <= io_inputBits;
          end
        end
      end
    end
    if (_T_241) begin
      outputMemBlock_0 <= _T_273;
    end
    if (_T_241) begin
      outputMemBlock_1 <= _T_272;
    end
    if (_T_241) begin
      outputMemBlock_2 <= _T_271;
    end
    if (_T_241) begin
      outputMemBlock_3 <= _T_270;
    end
    if (_T_241) begin
      outputMemBlock_4 <= _T_269;
    end
    if (_T_241) begin
      outputMemBlock_5 <= _T_268;
    end
    if (_T_241) begin
      outputMemBlock_6 <= _T_267;
    end
    if (_T_241) begin
      outputMemBlock_7 <= _T_266;
    end
    if (_T_241) begin
      outputMemBlock_8 <= _T_265;
    end
    if (_T_241) begin
      outputMemBlock_9 <= _T_264;
    end
    if (_T_241) begin
      outputMemBlock_10 <= _T_263;
    end
    if (_T_241) begin
      outputMemBlock_11 <= _T_262;
    end
    if (_T_241) begin
      outputMemBlock_12 <= _T_261;
    end
    if (_T_241) begin
      outputMemBlock_13 <= _T_260;
    end
    if (_T_241) begin
      outputMemBlock_14 <= _T_259;
    end
    if (_T_241) begin
      outputMemBlock_15 <= _T_258;
    end
    if (_T_241) begin
      outputMemBlock_16 <= _T_257;
    end
    if (_T_241) begin
      outputMemBlock_17 <= _T_256;
    end
    if (_T_241) begin
      outputMemBlock_18 <= _T_255;
    end
    if (_T_241) begin
      outputMemBlock_19 <= _T_254;
    end
    if (_T_241) begin
      outputMemBlock_20 <= _T_253;
    end
    if (_T_241) begin
      outputMemBlock_21 <= _T_252;
    end
    if (_T_241) begin
      outputMemBlock_22 <= _T_251;
    end
    if (_T_241) begin
      outputMemBlock_23 <= _T_250;
    end
    if (_T_241) begin
      outputMemBlock_24 <= _T_249;
    end
    if (_T_241) begin
      outputMemBlock_25 <= _T_248;
    end
    if (_T_241) begin
      outputMemBlock_26 <= _T_247;
    end
    if (_T_241) begin
      outputMemBlock_27 <= _T_246;
    end
    if (_T_241) begin
      outputMemBlock_28 <= _T_245;
    end
    if (_T_241) begin
      outputMemBlock_29 <= _T_244;
    end
    if (_T_241) begin
      outputMemBlock_30 <= _T_243;
    end
    if (_T_241) begin
      outputMemBlock_31 <= _T_242;
    end
    if (reset) begin
      outputBits <= 11'h0;
    end else begin
      if (_T_350) begin
        if (_T_355) begin
          outputBits <= 11'h0;
        end else begin
          if (_T_241) begin
            if (inner_io_outputValid) begin
              outputBits <= _T_289;
            end
          end
        end
      end else begin
        if (_T_241) begin
          if (inner_io_outputValid) begin
            outputBits <= _T_289;
          end
        end
      end
    end
    if (reset) begin
      outputPieceBits <= 6'h0;
    end else begin
      if (_T_241) begin
        if (_T_275) begin
          outputPieceBits <= 6'h20;
        end else begin
          outputPieceBits <= _T_279;
        end
      end
    end
    if (reset) begin
      inputReadAddr <= 5'h0;
    end else begin
      if (_T_121) begin
        if (!(_T_134)) begin
          inputReadAddr <= _T_137;
        end
      end
    end
    if (reset) begin
      outputWriteAddr <= 5'h0;
    end else begin
      if (_T_241) begin
        if (_T_275) begin
          outputWriteAddr <= _T_285;
        end
      end
    end
    if (reset) begin
      outputReadAddr <= 5'h0;
    end else begin
      if (_T_367) begin
        outputReadAddr <= 5'h0;
      end else begin
        if (_T_350) begin
          outputReadAddr <= _T_353;
        end
      end
    end
    if (reset) begin
      outputReadingStartedPrev <= 1'h0;
    end else begin
      if (_T_367) begin
        outputReadingStartedPrev <= 1'h0;
      end else begin
        if (_T_339) begin
          outputReadingStartedPrev <= 1'h1;
        end
      end
    end
    if (reset) begin
      outputReadingStarted <= 1'h0;
    end else begin
      if (_T_367) begin
        outputReadingStarted <= 1'h0;
      end else begin
        if (_T_343) begin
          outputReadingStarted <= 1'h1;
        end
      end
    end
  end
endmodule
module StreamingCore(
  input         clock,
  input         reset,
  input  [31:0] io_metadataPtr,
  output [31:0] io_inputMemAddr,
  output        io_inputMemAddrValid,
  output        io_inputMemAddrsFinished,
  input  [31:0] io_inputMemBlock,
  input  [4:0]  io_inputMemIdx,
  input         io_inputMemBlockValid,
  output [31:0] io_outputMemAddr,
  output        io_outputMemAddrValid,
  input         io_outputMemAddrReady,
  output [31:0] io_outputMemBlock,
  output [4:0]  io_outputMemIdx,
  output        io_outputMemBlockValid,
  output        io_outputFinished
);
  wire  core_clock;
  wire  core_reset;
  wire [31:0] core_io_inputMemBlock;
  wire [4:0] core_io_inputMemIdx;
  wire  core_io_inputMemBlockValid;
  wire [10:0] core_io_inputBits;
  wire  core_io_inputMemConsumed;
  wire  core_io_inputFinished;
  wire [31:0] core_io_outputMemBlock;
  wire  core_io_outputMemBlockValid;
  wire  core_io_outputMemBlockReady;
  wire [10:0] core_io_outputBits;
  wire  core_io_outputFinished;
  reg  isInit;
  reg [31:0] _RAND_0;
  reg  initDone;
  reg [31:0] _RAND_1;
  reg [31:0] inputBitsRemaining;
  reg [31:0] _RAND_2;
  reg [31:0] outputBits;
  reg [31:0] _RAND_3;
  reg [4:0] outputBlockCounter;
  reg [31:0] _RAND_4;
  reg  outputLengthSent;
  reg [31:0] _RAND_5;
  reg [31:0] inputMemAddr;
  reg [31:0] _RAND_6;
  reg [31:0] outputMemAddr;
  reg [31:0] _RAND_7;
  reg [31:0] outputLenAddr;
  reg [31:0] _RAND_8;
  wire  _T_35;
  wire  _T_37;
  wire  _T_39;
  wire  _T_40;
  wire  _T_41;
  wire  _T_44;
  wire  _T_46;
  wire [31:0] _T_48;
  wire  _T_50;
  wire [31:0] _GEN_0;
  wire  _T_53;
  wire [31:0] _GEN_1;
  wire  _T_56;
  wire [31:0] _GEN_2;
  wire [31:0] _GEN_3;
  wire  _T_60;
  wire [32:0] _T_64;
  wire [31:0] _T_65;
  wire  _GEN_4;
  wire  _GEN_5;
  wire [31:0] _GEN_6;
  wire  _T_67;
  wire [32:0] _T_71;
  wire [32:0] _T_72;
  wire [31:0] _T_73;
  wire [31:0] _T_75;
  wire [32:0] _T_77;
  wire [31:0] _T_78;
  wire [31:0] _GEN_7;
  wire [31:0] _GEN_8;
  wire [31:0] _GEN_9;
  wire [31:0] _GEN_10;
  wire [31:0] _GEN_11;
  wire [31:0] _GEN_12;
  wire  _GEN_13;
  wire  _GEN_14;
  wire [31:0] _GEN_15;
  wire [31:0] _GEN_16;
  wire [31:0] _GEN_17;
  wire [31:0] _GEN_18;
  wire  _GEN_19;
  wire  _GEN_20;
  wire  _T_81;
  reg  outputAddressAccepted;
  reg [31:0] _RAND_9;
  reg  outputAddressAcceptedNext;
  reg [31:0] _RAND_10;
  wire  _GEN_21;
  wire [31:0] _T_87;
  wire  _T_89;
  wire  _T_91;
  wire  _T_92;
  wire  _T_93;
  wire  _T_94;
  wire  _T_95;
  wire  _T_98;
  wire [31:0] _GEN_33;
  wire [32:0] _T_99;
  wire [31:0] _T_100;
  wire [31:0] _GEN_22;
  wire [31:0] _GEN_23;
  wire  _GEN_24;
  wire  _GEN_25;
  wire [31:0] _GEN_26;
  wire [31:0] _GEN_27;
  wire  _GEN_28;
  wire  _T_106;
  wire [5:0] _T_109;
  wire [4:0] _T_110;
  wire [4:0] _GEN_29;
  wire [4:0] _GEN_30;
  wire [31:0] outputBitsBlock;
  wire  _T_113;
  wire [31:0] tmp;
  wire [31:0] _T_116;
  wire  _T_119;
  wire  _GEN_31;
  wire  _GEN_32;
  wire  _T_124;
  InnerCore core (
    .clock(core_clock),
    .reset(core_reset),
    .io_inputMemBlock(core_io_inputMemBlock),
    .io_inputMemIdx(core_io_inputMemIdx),
    .io_inputMemBlockValid(core_io_inputMemBlockValid),
    .io_inputBits(core_io_inputBits),
    .io_inputMemConsumed(core_io_inputMemConsumed),
    .io_inputFinished(core_io_inputFinished),
    .io_outputMemBlock(core_io_outputMemBlock),
    .io_outputMemBlockValid(core_io_outputMemBlockValid),
    .io_outputMemBlockReady(core_io_outputMemBlockReady),
    .io_outputBits(core_io_outputBits),
    .io_outputFinished(core_io_outputFinished)
  );
  assign _T_35 = initDone & core_io_inputMemConsumed;
  assign _T_37 = inputBitsRemaining == 32'h0;
  assign _T_39 = _T_37 == 1'h0;
  assign _T_40 = _T_35 & _T_39;
  assign _T_41 = isInit | _T_40;
  assign _T_44 = io_inputMemBlockValid & initDone;
  assign _T_46 = inputBitsRemaining > 32'h400;
  assign _T_48 = _T_46 ? 32'h400 : inputBitsRemaining;
  assign _T_50 = io_inputMemIdx == 5'h0;
  assign _GEN_0 = _T_50 ? io_inputMemBlock : inputMemAddr;
  assign _T_53 = io_inputMemIdx == 5'h2;
  assign _GEN_1 = _T_53 ? io_inputMemBlock : inputBitsRemaining;
  assign _T_56 = io_inputMemIdx == 5'h4;
  assign _GEN_2 = _T_56 ? io_inputMemBlock : outputMemAddr;
  assign _GEN_3 = _T_56 ? io_inputMemBlock : outputLenAddr;
  assign _T_60 = io_inputMemIdx == 5'h1f;
  assign _T_64 = outputMemAddr + 32'h80;
  assign _T_65 = _T_64[31:0];
  assign _GEN_4 = _T_60 ? 1'h0 : isInit;
  assign _GEN_5 = _T_60 ? 1'h1 : initDone;
  assign _GEN_6 = _T_60 ? _T_65 : _GEN_2;
  assign _T_67 = io_inputMemIdx == 5'h1;
  assign _T_71 = inputBitsRemaining - 32'h400;
  assign _T_72 = $unsigned(_T_71);
  assign _T_73 = _T_72[31:0];
  assign _T_75 = _T_46 ? _T_73 : 32'h0;
  assign _T_77 = inputMemAddr + 32'h80;
  assign _T_78 = _T_77[31:0];
  assign _GEN_7 = _T_67 ? _T_75 : inputBitsRemaining;
  assign _GEN_8 = _T_67 ? _T_78 : inputMemAddr;
  assign _GEN_9 = isInit ? _GEN_0 : _GEN_8;
  assign _GEN_10 = isInit ? _GEN_1 : _GEN_7;
  assign _GEN_11 = isInit ? _GEN_6 : outputMemAddr;
  assign _GEN_12 = isInit ? _GEN_3 : outputLenAddr;
  assign _GEN_13 = isInit ? _GEN_4 : isInit;
  assign _GEN_14 = isInit ? _GEN_5 : initDone;
  assign _GEN_15 = io_inputMemBlockValid ? _GEN_9 : inputMemAddr;
  assign _GEN_16 = io_inputMemBlockValid ? _GEN_10 : inputBitsRemaining;
  assign _GEN_17 = io_inputMemBlockValid ? _GEN_11 : outputMemAddr;
  assign _GEN_18 = io_inputMemBlockValid ? _GEN_12 : outputLenAddr;
  assign _GEN_19 = io_inputMemBlockValid ? _GEN_13 : isInit;
  assign _GEN_20 = io_inputMemBlockValid ? _GEN_14 : initDone;
  assign _T_81 = initDone & _T_37;
  assign _GEN_21 = outputAddressAccepted ? 1'h1 : outputAddressAcceptedNext;
  assign _T_87 = core_io_outputFinished ? outputLenAddr : outputMemAddr;
  assign _T_89 = outputAddressAccepted == 1'h0;
  assign _T_91 = outputLengthSent == 1'h0;
  assign _T_92 = core_io_outputFinished & _T_91;
  assign _T_93 = core_io_outputMemBlockValid | _T_92;
  assign _T_94 = _T_89 & _T_93;
  assign _T_95 = io_outputMemAddrValid & io_outputMemAddrReady;
  assign _T_98 = core_io_outputFinished == 1'h0;
  assign _GEN_33 = {{21'd0}, core_io_outputBits};
  assign _T_99 = outputBits + _GEN_33;
  assign _T_100 = _T_99[31:0];
  assign _GEN_22 = _T_98 ? _T_100 : outputBits;
  assign _GEN_23 = _T_98 ? _T_65 : _GEN_17;
  assign _GEN_24 = _T_98 ? outputLengthSent : 1'h1;
  assign _GEN_25 = _T_95 ? 1'h1 : outputAddressAccepted;
  assign _GEN_26 = _T_95 ? _GEN_22 : outputBits;
  assign _GEN_27 = _T_95 ? _GEN_23 : _GEN_17;
  assign _GEN_28 = _T_95 ? _GEN_24 : outputLengthSent;
  assign _T_106 = outputBlockCounter == 5'h1f;
  assign _T_109 = outputBlockCounter + 5'h1;
  assign _T_110 = _T_109[4:0];
  assign _GEN_29 = _T_106 ? 5'h0 : _T_110;
  assign _GEN_30 = outputAddressAcceptedNext ? _GEN_29 : outputBlockCounter;
  assign _T_113 = outputBlockCounter == 5'h0;
  assign tmp = _T_113 ? outputBits : 32'h0;
  assign _T_116 = outputLengthSent ? outputBitsBlock : core_io_outputMemBlock;
  assign _T_119 = outputAddressAcceptedNext & _T_106;
  assign _GEN_31 = _T_119 ? 1'h0 : _GEN_25;
  assign _GEN_32 = _T_119 ? 1'h0 : _GEN_21;
  assign _T_124 = outputLengthSent & _T_89;
  assign io_inputMemAddr = inputMemAddr;
  assign io_inputMemAddrValid = _T_41;
  assign io_inputMemAddrsFinished = _T_37;
  assign io_outputMemAddr = _T_87;
  assign io_outputMemAddrValid = _T_94;
  assign io_outputMemBlock = _T_116;
  assign io_outputMemIdx = outputBlockCounter;
  assign io_outputMemBlockValid = outputAddressAcceptedNext;
  assign io_outputFinished = _T_124;
  assign core_io_inputMemBlock = io_inputMemBlock;
  assign core_io_inputMemIdx = io_inputMemIdx;
  assign core_io_inputMemBlockValid = _T_44;
  assign core_io_inputBits = _T_48[10:0];
  assign core_io_inputFinished = _T_81;
  assign core_io_outputMemBlockReady = outputAddressAccepted;
  assign core_clock = clock;
  assign core_reset = reset;
  assign outputBitsBlock = tmp;
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{$random}};
  isInit = _RAND_0[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{$random}};
  initDone = _RAND_1[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{$random}};
  inputBitsRemaining = _RAND_2[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{$random}};
  outputBits = _RAND_3[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_4 = {1{$random}};
  outputBlockCounter = _RAND_4[4:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_5 = {1{$random}};
  outputLengthSent = _RAND_5[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_6 = {1{$random}};
  inputMemAddr = _RAND_6[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_7 = {1{$random}};
  outputMemAddr = _RAND_7[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_8 = {1{$random}};
  outputLenAddr = _RAND_8[31:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_9 = {1{$random}};
  outputAddressAccepted = _RAND_9[0:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_10 = {1{$random}};
  outputAddressAcceptedNext = _RAND_10[0:0];
  `endif // RANDOMIZE_REG_INIT
  end
`endif // RANDOMIZE
  always @(posedge clock) begin
    if (reset) begin
      isInit <= 1'h1;
    end else begin
      if (io_inputMemBlockValid) begin
        if (isInit) begin
          if (_T_60) begin
            isInit <= 1'h0;
          end
        end
      end
    end
    if (reset) begin
      initDone <= 1'h0;
    end else begin
      if (io_inputMemBlockValid) begin
        if (isInit) begin
          if (_T_60) begin
            initDone <= 1'h1;
          end
        end
      end
    end
    if (reset) begin
      inputBitsRemaining <= 32'h1;
    end else begin
      if (io_inputMemBlockValid) begin
        if (isInit) begin
          if (_T_53) begin
            inputBitsRemaining <= io_inputMemBlock;
          end
        end else begin
          if (_T_67) begin
            if (_T_46) begin
              inputBitsRemaining <= _T_73;
            end else begin
              inputBitsRemaining <= 32'h0;
            end
          end
        end
      end
    end
    if (reset) begin
      outputBits <= 32'h0;
    end else begin
      if (_T_95) begin
        if (_T_98) begin
          outputBits <= _T_100;
        end
      end
    end
    if (reset) begin
      outputBlockCounter <= 5'h0;
    end else begin
      if (outputAddressAcceptedNext) begin
        if (_T_106) begin
          outputBlockCounter <= 5'h0;
        end else begin
          outputBlockCounter <= _T_110;
        end
      end
    end
    if (reset) begin
      outputLengthSent <= 1'h0;
    end else begin
      if (_T_95) begin
        if (!(_T_98)) begin
          outputLengthSent <= 1'h1;
        end
      end
    end
    if (reset) begin
      inputMemAddr <= io_metadataPtr;
    end else begin
      if (io_inputMemBlockValid) begin
        if (isInit) begin
          if (_T_50) begin
            inputMemAddr <= io_inputMemBlock;
          end
        end else begin
          if (_T_67) begin
            inputMemAddr <= _T_78;
          end
        end
      end
    end
    if (_T_95) begin
      if (_T_98) begin
        outputMemAddr <= _T_65;
      end else begin
        if (io_inputMemBlockValid) begin
          if (isInit) begin
            if (_T_60) begin
              outputMemAddr <= _T_65;
            end else begin
              if (_T_56) begin
                outputMemAddr <= io_inputMemBlock;
              end
            end
          end
        end
      end
    end else begin
      if (io_inputMemBlockValid) begin
        if (isInit) begin
          if (_T_60) begin
            outputMemAddr <= _T_65;
          end else begin
            if (_T_56) begin
              outputMemAddr <= io_inputMemBlock;
            end
          end
        end
      end
    end
    if (io_inputMemBlockValid) begin
      if (isInit) begin
        if (_T_56) begin
          outputLenAddr <= io_inputMemBlock;
        end
      end
    end
    if (reset) begin
      outputAddressAccepted <= 1'h0;
    end else begin
      if (_T_119) begin
        outputAddressAccepted <= 1'h0;
      end else begin
        if (_T_95) begin
          outputAddressAccepted <= 1'h1;
        end
      end
    end
    if (reset) begin
      outputAddressAcceptedNext <= 1'h0;
    end else begin
      if (_T_119) begin
        outputAddressAcceptedNext <= 1'h0;
      end else begin
        if (outputAddressAccepted) begin
          outputAddressAcceptedNext <= 1'h1;
        end
      end
    end
  end
endmodule
module StreamingWrapperBB(
  input          clock,
  input          reset,
  output [63:0]  axi_inputMemAddrs_0,
  output         axi_inputMemAddrValids_0,
  output [7:0]   axi_inputMemAddrLens_0,
  input          axi_inputMemAddrReadys_0,
  input  [511:0] axi_inputMemBlocks_0,
  input          axi_inputMemBlockValids_0,
  output         axi_inputMemBlockReadys_0,
  output [63:0]  axi_outputMemAddrs_0,
  output         axi_outputMemAddrValids_0,
  output [7:0]   axi_outputMemAddrLens_0,
  output [15:0]  axi_outputMemAddrIds_0,
  input          axi_outputMemAddrReadys_0,
  output [511:0] axi_outputMemBlocks_0,
  output         axi_outputMemBlockValids_0,
  output         axi_outputMemBlockLasts_0,
  input          axi_outputMemBlockReadys_0,
  output         axi_finished
);
  wire  StreamingMemoryController_clock;
  wire  StreamingMemoryController_reset;
  wire [63:0] StreamingMemoryController_io_axi_inputMemAddrs_0;
  wire  StreamingMemoryController_io_axi_inputMemAddrValids_0;
  wire [7:0] StreamingMemoryController_io_axi_inputMemAddrLens_0;
  wire  StreamingMemoryController_io_axi_inputMemAddrReadys_0;
  wire [511:0] StreamingMemoryController_io_axi_inputMemBlocks_0;
  wire  StreamingMemoryController_io_axi_inputMemBlockValids_0;
  wire  StreamingMemoryController_io_axi_inputMemBlockReadys_0;
  wire [63:0] StreamingMemoryController_io_axi_outputMemAddrs_0;
  wire  StreamingMemoryController_io_axi_outputMemAddrValids_0;
  wire [7:0] StreamingMemoryController_io_axi_outputMemAddrLens_0;
  wire [15:0] StreamingMemoryController_io_axi_outputMemAddrIds_0;
  wire  StreamingMemoryController_io_axi_outputMemAddrReadys_0;
  wire [511:0] StreamingMemoryController_io_axi_outputMemBlocks_0;
  wire  StreamingMemoryController_io_axi_outputMemBlockValids_0;
  wire  StreamingMemoryController_io_axi_outputMemBlockLasts_0;
  wire  StreamingMemoryController_io_axi_outputMemBlockReadys_0;
  wire  StreamingMemoryController_io_axi_finished;
  wire [31:0] StreamingMemoryController_io_streamingCores_0_metadataPtr;
  wire [31:0] StreamingMemoryController_io_streamingCores_0_inputMemAddr;
  wire  StreamingMemoryController_io_streamingCores_0_inputMemAddrValid;
  wire  StreamingMemoryController_io_streamingCores_0_inputMemAddrsFinished;
  wire [31:0] StreamingMemoryController_io_streamingCores_0_inputMemBlock;
  wire [4:0] StreamingMemoryController_io_streamingCores_0_inputMemIdx;
  wire  StreamingMemoryController_io_streamingCores_0_inputMemBlockValid;
  wire [31:0] StreamingMemoryController_io_streamingCores_0_outputMemAddr;
  wire  StreamingMemoryController_io_streamingCores_0_outputMemAddrValid;
  wire  StreamingMemoryController_io_streamingCores_0_outputMemAddrReady;
  wire [31:0] StreamingMemoryController_io_streamingCores_0_outputMemBlock;
  wire [4:0] StreamingMemoryController_io_streamingCores_0_outputMemIdx;
  wire  StreamingMemoryController_io_streamingCores_0_outputMemBlockValid;
  wire  StreamingMemoryController_io_streamingCores_0_outputFinished;
  wire [31:0] StreamingMemoryController_io_streamingCores_1_metadataPtr;
  wire [31:0] StreamingMemoryController_io_streamingCores_1_inputMemAddr;
  wire  StreamingMemoryController_io_streamingCores_1_inputMemAddrValid;
  wire  StreamingMemoryController_io_streamingCores_1_inputMemAddrsFinished;
  wire [31:0] StreamingMemoryController_io_streamingCores_1_inputMemBlock;
  wire [4:0] StreamingMemoryController_io_streamingCores_1_inputMemIdx;
  wire  StreamingMemoryController_io_streamingCores_1_inputMemBlockValid;
  wire [31:0] StreamingMemoryController_io_streamingCores_1_outputMemAddr;
  wire  StreamingMemoryController_io_streamingCores_1_outputMemAddrValid;
  wire  StreamingMemoryController_io_streamingCores_1_outputMemAddrReady;
  wire [31:0] StreamingMemoryController_io_streamingCores_1_outputMemBlock;
  wire [4:0] StreamingMemoryController_io_streamingCores_1_outputMemIdx;
  wire  StreamingMemoryController_io_streamingCores_1_outputMemBlockValid;
  wire  StreamingMemoryController_io_streamingCores_1_outputFinished;
  wire  StreamingCore_clock;
  wire  StreamingCore_reset;
  wire [31:0] StreamingCore_io_metadataPtr;
  wire [31:0] StreamingCore_io_inputMemAddr;
  wire  StreamingCore_io_inputMemAddrValid;
  wire  StreamingCore_io_inputMemAddrsFinished;
  wire [31:0] StreamingCore_io_inputMemBlock;
  wire [4:0] StreamingCore_io_inputMemIdx;
  wire  StreamingCore_io_inputMemBlockValid;
  wire [31:0] StreamingCore_io_outputMemAddr;
  wire  StreamingCore_io_outputMemAddrValid;
  wire  StreamingCore_io_outputMemAddrReady;
  wire [31:0] StreamingCore_io_outputMemBlock;
  wire [4:0] StreamingCore_io_outputMemIdx;
  wire  StreamingCore_io_outputMemBlockValid;
  wire  StreamingCore_io_outputFinished;
  wire  StreamingCore_1_clock;
  wire  StreamingCore_1_reset;
  wire [31:0] StreamingCore_1_io_metadataPtr;
  wire [31:0] StreamingCore_1_io_inputMemAddr;
  wire  StreamingCore_1_io_inputMemAddrValid;
  wire  StreamingCore_1_io_inputMemAddrsFinished;
  wire [31:0] StreamingCore_1_io_inputMemBlock;
  wire [4:0] StreamingCore_1_io_inputMemIdx;
  wire  StreamingCore_1_io_inputMemBlockValid;
  wire [31:0] StreamingCore_1_io_outputMemAddr;
  wire  StreamingCore_1_io_outputMemAddrValid;
  wire  StreamingCore_1_io_outputMemAddrReady;
  wire [31:0] StreamingCore_1_io_outputMemBlock;
  wire [4:0] StreamingCore_1_io_outputMemIdx;
  wire  StreamingCore_1_io_outputMemBlockValid;
  wire  StreamingCore_1_io_outputFinished;
  StreamingMemoryController StreamingMemoryController (
    .clock(StreamingMemoryController_clock),
    .reset(StreamingMemoryController_reset),
    .io_axi_inputMemAddrs_0(StreamingMemoryController_io_axi_inputMemAddrs_0),
    .io_axi_inputMemAddrValids_0(StreamingMemoryController_io_axi_inputMemAddrValids_0),
    .io_axi_inputMemAddrLens_0(StreamingMemoryController_io_axi_inputMemAddrLens_0),
    .io_axi_inputMemAddrReadys_0(StreamingMemoryController_io_axi_inputMemAddrReadys_0),
    .io_axi_inputMemBlocks_0(StreamingMemoryController_io_axi_inputMemBlocks_0),
    .io_axi_inputMemBlockValids_0(StreamingMemoryController_io_axi_inputMemBlockValids_0),
    .io_axi_inputMemBlockReadys_0(StreamingMemoryController_io_axi_inputMemBlockReadys_0),
    .io_axi_outputMemAddrs_0(StreamingMemoryController_io_axi_outputMemAddrs_0),
    .io_axi_outputMemAddrValids_0(StreamingMemoryController_io_axi_outputMemAddrValids_0),
    .io_axi_outputMemAddrLens_0(StreamingMemoryController_io_axi_outputMemAddrLens_0),
    .io_axi_outputMemAddrIds_0(StreamingMemoryController_io_axi_outputMemAddrIds_0),
    .io_axi_outputMemAddrReadys_0(StreamingMemoryController_io_axi_outputMemAddrReadys_0),
    .io_axi_outputMemBlocks_0(StreamingMemoryController_io_axi_outputMemBlocks_0),
    .io_axi_outputMemBlockValids_0(StreamingMemoryController_io_axi_outputMemBlockValids_0),
    .io_axi_outputMemBlockLasts_0(StreamingMemoryController_io_axi_outputMemBlockLasts_0),
    .io_axi_outputMemBlockReadys_0(StreamingMemoryController_io_axi_outputMemBlockReadys_0),
    .io_axi_finished(StreamingMemoryController_io_axi_finished),
    .io_streamingCores_0_metadataPtr(StreamingMemoryController_io_streamingCores_0_metadataPtr),
    .io_streamingCores_0_inputMemAddr(StreamingMemoryController_io_streamingCores_0_inputMemAddr),
    .io_streamingCores_0_inputMemAddrValid(StreamingMemoryController_io_streamingCores_0_inputMemAddrValid),
    .io_streamingCores_0_inputMemAddrsFinished(StreamingMemoryController_io_streamingCores_0_inputMemAddrsFinished),
    .io_streamingCores_0_inputMemBlock(StreamingMemoryController_io_streamingCores_0_inputMemBlock),
    .io_streamingCores_0_inputMemIdx(StreamingMemoryController_io_streamingCores_0_inputMemIdx),
    .io_streamingCores_0_inputMemBlockValid(StreamingMemoryController_io_streamingCores_0_inputMemBlockValid),
    .io_streamingCores_0_outputMemAddr(StreamingMemoryController_io_streamingCores_0_outputMemAddr),
    .io_streamingCores_0_outputMemAddrValid(StreamingMemoryController_io_streamingCores_0_outputMemAddrValid),
    .io_streamingCores_0_outputMemAddrReady(StreamingMemoryController_io_streamingCores_0_outputMemAddrReady),
    .io_streamingCores_0_outputMemBlock(StreamingMemoryController_io_streamingCores_0_outputMemBlock),
    .io_streamingCores_0_outputMemIdx(StreamingMemoryController_io_streamingCores_0_outputMemIdx),
    .io_streamingCores_0_outputMemBlockValid(StreamingMemoryController_io_streamingCores_0_outputMemBlockValid),
    .io_streamingCores_0_outputFinished(StreamingMemoryController_io_streamingCores_0_outputFinished),
    .io_streamingCores_1_metadataPtr(StreamingMemoryController_io_streamingCores_1_metadataPtr),
    .io_streamingCores_1_inputMemAddr(StreamingMemoryController_io_streamingCores_1_inputMemAddr),
    .io_streamingCores_1_inputMemAddrValid(StreamingMemoryController_io_streamingCores_1_inputMemAddrValid),
    .io_streamingCores_1_inputMemAddrsFinished(StreamingMemoryController_io_streamingCores_1_inputMemAddrsFinished),
    .io_streamingCores_1_inputMemBlock(StreamingMemoryController_io_streamingCores_1_inputMemBlock),
    .io_streamingCores_1_inputMemIdx(StreamingMemoryController_io_streamingCores_1_inputMemIdx),
    .io_streamingCores_1_inputMemBlockValid(StreamingMemoryController_io_streamingCores_1_inputMemBlockValid),
    .io_streamingCores_1_outputMemAddr(StreamingMemoryController_io_streamingCores_1_outputMemAddr),
    .io_streamingCores_1_outputMemAddrValid(StreamingMemoryController_io_streamingCores_1_outputMemAddrValid),
    .io_streamingCores_1_outputMemAddrReady(StreamingMemoryController_io_streamingCores_1_outputMemAddrReady),
    .io_streamingCores_1_outputMemBlock(StreamingMemoryController_io_streamingCores_1_outputMemBlock),
    .io_streamingCores_1_outputMemIdx(StreamingMemoryController_io_streamingCores_1_outputMemIdx),
    .io_streamingCores_1_outputMemBlockValid(StreamingMemoryController_io_streamingCores_1_outputMemBlockValid),
    .io_streamingCores_1_outputFinished(StreamingMemoryController_io_streamingCores_1_outputFinished)
  );
  StreamingCore StreamingCore (
    .clock(StreamingCore_clock),
    .reset(StreamingCore_reset),
    .io_metadataPtr(StreamingCore_io_metadataPtr),
    .io_inputMemAddr(StreamingCore_io_inputMemAddr),
    .io_inputMemAddrValid(StreamingCore_io_inputMemAddrValid),
    .io_inputMemAddrsFinished(StreamingCore_io_inputMemAddrsFinished),
    .io_inputMemBlock(StreamingCore_io_inputMemBlock),
    .io_inputMemIdx(StreamingCore_io_inputMemIdx),
    .io_inputMemBlockValid(StreamingCore_io_inputMemBlockValid),
    .io_outputMemAddr(StreamingCore_io_outputMemAddr),
    .io_outputMemAddrValid(StreamingCore_io_outputMemAddrValid),
    .io_outputMemAddrReady(StreamingCore_io_outputMemAddrReady),
    .io_outputMemBlock(StreamingCore_io_outputMemBlock),
    .io_outputMemIdx(StreamingCore_io_outputMemIdx),
    .io_outputMemBlockValid(StreamingCore_io_outputMemBlockValid),
    .io_outputFinished(StreamingCore_io_outputFinished)
  );
  StreamingCore StreamingCore_1 (
    .clock(StreamingCore_1_clock),
    .reset(StreamingCore_1_reset),
    .io_metadataPtr(StreamingCore_1_io_metadataPtr),
    .io_inputMemAddr(StreamingCore_1_io_inputMemAddr),
    .io_inputMemAddrValid(StreamingCore_1_io_inputMemAddrValid),
    .io_inputMemAddrsFinished(StreamingCore_1_io_inputMemAddrsFinished),
    .io_inputMemBlock(StreamingCore_1_io_inputMemBlock),
    .io_inputMemIdx(StreamingCore_1_io_inputMemIdx),
    .io_inputMemBlockValid(StreamingCore_1_io_inputMemBlockValid),
    .io_outputMemAddr(StreamingCore_1_io_outputMemAddr),
    .io_outputMemAddrValid(StreamingCore_1_io_outputMemAddrValid),
    .io_outputMemAddrReady(StreamingCore_1_io_outputMemAddrReady),
    .io_outputMemBlock(StreamingCore_1_io_outputMemBlock),
    .io_outputMemIdx(StreamingCore_1_io_outputMemIdx),
    .io_outputMemBlockValid(StreamingCore_1_io_outputMemBlockValid),
    .io_outputFinished(StreamingCore_1_io_outputFinished)
  );
  assign axi_inputMemAddrs_0 = StreamingMemoryController_io_axi_inputMemAddrs_0;
  assign axi_inputMemAddrValids_0 = StreamingMemoryController_io_axi_inputMemAddrValids_0;
  assign axi_inputMemAddrLens_0 = StreamingMemoryController_io_axi_inputMemAddrLens_0;
  assign axi_inputMemBlockReadys_0 = StreamingMemoryController_io_axi_inputMemBlockReadys_0;
  assign axi_outputMemAddrs_0 = StreamingMemoryController_io_axi_outputMemAddrs_0;
  assign axi_outputMemAddrValids_0 = StreamingMemoryController_io_axi_outputMemAddrValids_0;
  assign axi_outputMemAddrLens_0 = StreamingMemoryController_io_axi_outputMemAddrLens_0;
  assign axi_outputMemAddrIds_0 = StreamingMemoryController_io_axi_outputMemAddrIds_0;
  assign axi_outputMemBlocks_0 = StreamingMemoryController_io_axi_outputMemBlocks_0;
  assign axi_outputMemBlockValids_0 = StreamingMemoryController_io_axi_outputMemBlockValids_0;
  assign axi_outputMemBlockLasts_0 = StreamingMemoryController_io_axi_outputMemBlockLasts_0;
  assign axi_finished = StreamingMemoryController_io_axi_finished;
  assign StreamingMemoryController_io_axi_inputMemAddrReadys_0 = axi_inputMemAddrReadys_0;
  assign StreamingMemoryController_io_axi_inputMemBlocks_0 = axi_inputMemBlocks_0;
  assign StreamingMemoryController_io_axi_inputMemBlockValids_0 = axi_inputMemBlockValids_0;
  assign StreamingMemoryController_io_axi_outputMemAddrReadys_0 = axi_outputMemAddrReadys_0;
  assign StreamingMemoryController_io_axi_outputMemBlockReadys_0 = axi_outputMemBlockReadys_0;
  assign StreamingMemoryController_io_streamingCores_0_inputMemAddr = StreamingCore_io_inputMemAddr;
  assign StreamingMemoryController_io_streamingCores_0_inputMemAddrValid = StreamingCore_io_inputMemAddrValid;
  assign StreamingMemoryController_io_streamingCores_0_inputMemAddrsFinished = StreamingCore_io_inputMemAddrsFinished;
  assign StreamingMemoryController_io_streamingCores_0_outputMemAddr = StreamingCore_io_outputMemAddr;
  assign StreamingMemoryController_io_streamingCores_0_outputMemAddrValid = StreamingCore_io_outputMemAddrValid;
  assign StreamingMemoryController_io_streamingCores_0_outputMemBlock = StreamingCore_io_outputMemBlock;
  assign StreamingMemoryController_io_streamingCores_0_outputMemIdx = StreamingCore_io_outputMemIdx;
  assign StreamingMemoryController_io_streamingCores_0_outputMemBlockValid = StreamingCore_io_outputMemBlockValid;
  assign StreamingMemoryController_io_streamingCores_0_outputFinished = StreamingCore_io_outputFinished;
  assign StreamingMemoryController_io_streamingCores_1_inputMemAddr = StreamingCore_1_io_inputMemAddr;
  assign StreamingMemoryController_io_streamingCores_1_inputMemAddrValid = StreamingCore_1_io_inputMemAddrValid;
  assign StreamingMemoryController_io_streamingCores_1_inputMemAddrsFinished = StreamingCore_1_io_inputMemAddrsFinished;
  assign StreamingMemoryController_io_streamingCores_1_outputMemAddr = StreamingCore_1_io_outputMemAddr;
  assign StreamingMemoryController_io_streamingCores_1_outputMemAddrValid = StreamingCore_1_io_outputMemAddrValid;
  assign StreamingMemoryController_io_streamingCores_1_outputMemBlock = StreamingCore_1_io_outputMemBlock;
  assign StreamingMemoryController_io_streamingCores_1_outputMemIdx = StreamingCore_1_io_outputMemIdx;
  assign StreamingMemoryController_io_streamingCores_1_outputMemBlockValid = StreamingCore_1_io_outputMemBlockValid;
  assign StreamingMemoryController_io_streamingCores_1_outputFinished = StreamingCore_1_io_outputFinished;
  assign StreamingMemoryController_clock = clock;
  assign StreamingMemoryController_reset = reset;
  assign StreamingCore_io_metadataPtr = StreamingMemoryController_io_streamingCores_0_metadataPtr;
  assign StreamingCore_io_inputMemBlock = StreamingMemoryController_io_streamingCores_0_inputMemBlock;
  assign StreamingCore_io_inputMemIdx = StreamingMemoryController_io_streamingCores_0_inputMemIdx;
  assign StreamingCore_io_inputMemBlockValid = StreamingMemoryController_io_streamingCores_0_inputMemBlockValid;
  assign StreamingCore_io_outputMemAddrReady = StreamingMemoryController_io_streamingCores_0_outputMemAddrReady;
  assign StreamingCore_clock = clock;
  assign StreamingCore_reset = reset;
  assign StreamingCore_1_io_metadataPtr = StreamingMemoryController_io_streamingCores_1_metadataPtr;
  assign StreamingCore_1_io_inputMemBlock = StreamingMemoryController_io_streamingCores_1_inputMemBlock;
  assign StreamingCore_1_io_inputMemIdx = StreamingMemoryController_io_streamingCores_1_inputMemIdx;
  assign StreamingCore_1_io_inputMemBlockValid = StreamingMemoryController_io_streamingCores_1_inputMemBlockValid;
  assign StreamingCore_1_io_outputMemAddrReady = StreamingMemoryController_io_streamingCores_1_outputMemAddrReady;
  assign StreamingCore_1_clock = clock;
  assign StreamingCore_1_reset = reset;
endmodule
