#version 120
#ifdef GL_ES
    precision mediump float;
#endif


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;


uniform vec3 topColor;
uniform vec3 bottomColor;
uniform float parallax = 0.0; // +1.0: all top col, -1.0: all bototm col, 0.0: normal grad
uniform float parallax_size = 1.0/3.0; // 0: no parallax


// "steps" of R, G and B. Must be integer && equal or greater than 2
uniform float rcount = 64.0; // it even works on 256.0!
uniform float gcount = 64.0; // using 64: has less banding and most monitors are internally 6-bit
uniform float bcount = 64.0;


int bayer[25 * 25] = int[](165,530,106,302,540,219,477,100,231,417,314,223,424,37,207,434,326,22,448,338,111,454,523,278,579,334,19,410,495,57,352,158,318,598,109,509,157,524,282,606,83,225,539,163,234,607,313,206,71,470,251,608,216,135,275,609,415,29,451,204,397,21,373,107,462,348,482,120,362,508,33,147,572,388,142,447,77,345,565,439,104,215,546,279,69,567,311,585,258,177,17,266,601,55,428,270,461,331,26,560,164,271,486,186,16,336,457,150,342,471,245,161,56,396,496,555,385,146,321,190,526,97,182,511,297,429,553,49,374,536,263,575,43,501,124,368,538,450,121,309,84,210,449,561,79,356,610,256,378,58,105,315,156,244,423,118,183,408,220,611,15,198,293,596,221,375,581,39,238,500,287,14,437,139,595,227,403,590,478,68,612,295,517,87,312,413,515,78,433,13,476,134,340,414,160,466,213,547,324,456,542,141,12,335,214,357,11,381,242,469,159,265,383,176,545,285,197,503,108,576,51,387,98,200,34,358,489,277,570,96,441,554,123,534,52,556,112,605,330,70,392,613,28,288,361,232,602,300,502,267,102,195,399,152,484,264,166,289,427,192,298,407,25,249,520,114,233,444,543,170,498,131,452,66,562,310,586,54,531,346,42,614,354,23,588,491,151,468,353,187,483,369,153,85,425,10,276,371,174,420,32,459,222,304,136,421,103,458,230,339,67,260,578,93,544,9,280,594,327,248,582,472,50,615,254,537,359,91,600,475,212,525,168,558,128,455,370,179,301,405,209,467,48,442,127,355,184,332,481,126,286,175,436,273,31,377,306,36,412,294,616,8,473,60,603,116,347,532,191,568,61,522,90,218,391,592,62,514,122,552,149,617,241,513,81,202,272,557,333,226,507,255,72,305,402,229,418,296,551,7,411,317,236,416,337,480,64,389,132,350,487,404,89,162,435,44,419,618,113,505,20,604,138,465,188,493,133,580,6,169,259,320,548,193,593,40,178,512,364,591,144,319,196,386,261,351,205,384,76,269,38,349,208,504,440,99,490,5,426,243,322,574,281,4,237,460,527,3,549,155,577,47,533,316,619,394,519,82,268,325,566,199,299,119,529,75,400,125,492,344,86,217,308,463,80,395,284,474,117,201,95,235,422,620,143,45,372,597,453,343,185,479,247,569,171,409,584,129,365,239,488,94,224,438,559,283,541,18,194,401,516,262,148,41,250,621,24,329,92,446,27,291,485,35,622,180,535,379,30,341,443,145,363,494,246,101,445,550,390,499,115,432,521,211,623,253,528,189,430,307,53,323,130,624,172,46,589,292,63,599,328,203,74,290,181,376,274,140,393,59,367,88,380,137,506,252,571,431,240,497,382,228,464,167,398,2,573,366,518,1,583,73,563,303,510,154,564,257,587,65,406,173,0,360,110);
float bayerSize = 25.0;



float bayerDivider = bayerSize * bayerSize;


vec4 nearestColour(vec4 incolor) {
    vec4 rgbaCounts = vec4(rcount, gcount, bcount, 1.0);


    vec4 color = incolor;

    color.r = floor((rgbaCounts.r - 1.0) * color.r + 0.5) / (rgbaCounts.r - 1.0);
    color.g = floor((rgbaCounts.g - 1.0) * color.g + 0.5) / (rgbaCounts.g - 1.0);
    color.b = floor((rgbaCounts.b - 1.0) * color.b + 0.5) / (rgbaCounts.b - 1.0);
    color.a = 1.0;

    return color;
}

void main(void) {
    float spread = 1.0 / (0.299 * (rcount - 1.0) + 0.587 * (gcount - 1.0) + 0.114 * (bcount - 1.0));  // this spread value is optimised one -- try your own values for various effects!

    float scale = v_texCoords.y * (1.0 - parallax_size) + (parallax_size / 2.0) + (parallax * parallax_size / 2.0);


    float inR = mix(bottomColor.r, topColor.r, scale);
    float inG = mix(bottomColor.g, topColor.g, scale);
    float inB = mix(bottomColor.b, topColor.b, scale);

    vec4 inColor = vec4(inR, inG, inB, 1.0);

    vec2 entry = mod(gl_FragCoord.xy, vec2(bayerSize, bayerSize));

    vec4 outcolor = nearestColour(inColor + spread * (bayer[int(entry.y) * int(bayerSize) + int(entry.x)] / bayerDivider - 0.5));


    // New addition: centre lines
    if (
            (v_texCoords.x >= 0.5 && v_texCoords.x - 0.5 <= 0.001) ||
            (v_texCoords.y >= 0.5 && v_texCoords.y - 0.5 <= 0.001)
    ) {
        gl_FragColor = vec4(1 - outcolor.rgb, 1);
    }
    else {
        gl_FragColor = outcolor;
    }
}

/*
UV mapping coord.y

-+ <- 1.0  =
D|         = // parallax of +1
i|  =      =
s|  = // parallax of 0
p|  =      =
.|         = // parallax of -1
-+ <- 0.0  =
*/