public class ColorSpaceConverter {

    int[] RGBtoYUV(int r, int g, int b) {

        //The equations contain a bias/offset: + 16 for Y and + 128 for U and V.
        // This bias ensures that Y, U, and V values stay within the desired range.
        // In the YUV color space, Y values typically range from 16 to 235, while U and V range from 16 to 240.
        // Adding this bias helps to bring the values within these ranges.
        // (Prevents ArrayIndexOutOfBoundsException)

        int y,u,v;
        // Y component
        y = (int) (0.257 * r + 0.504 * g + 0.098 * b + 16);
        // U component
        u = (int) (-0.148 * r - 0.291 * g + 0.439 * b + 128);
        // V component
        v = (int) (0.439 * r - 0.368 * g - 0.071 * b + 128);

        return new int[] { y, u, v };
    }

}
