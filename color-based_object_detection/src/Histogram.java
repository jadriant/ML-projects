import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

public class Histogram {
    int WIDTH = 640;
    int HEIGHT = 480;
    private static final byte GREEN_CHROMA_R = 0;
    private static final byte GREEN_CHROMA_G = (byte)255; // Green is dominant
    private static final byte GREEN_CHROMA_B = 0;
    private static final int chroma_threshold = 20;
    public ColorSpaceConverter csc = new ColorSpaceConverter();


    /*
        HISTOGRAM CREATION PORTION
     */

    /*
        - The structure of int[][] histogram represents two histograms: one for the U channel and one for the V channel of the yuvValues color space.
        - The first row (index 0) is for the U channel, and the second row (index 1) is for the V channel.
        - Each row has 256 columns representing the frequency of each color value from 0 to 255.
     */
    public int[][] getHistogram(BufferedImage img, String imgType) {
        int[] uChannelHistogram = new int[256];
        int[] vChannelHistogram = new int[256];
        int[][] resHistogram = new int[256][256];

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Color clr = new Color(img.getRGB(x, y));
                if (!"object".equals(imgType) || !isGreenChroma(clr.getRed(), clr.getGreen(), clr.getBlue())) {
                    accumulateHistogram(clr, uChannelHistogram, vChannelHistogram);
                }
            }
        }
        resHistogram[0] = uChannelHistogram;
        resHistogram[1] = vChannelHistogram;
        return resHistogram;
    }
    private void accumulateHistogram(Color clr, int[] uChannelHistogram, int[] vChannelHistogram) {
        int[] yuvValues = csc.RGBtoYUV(clr.getRed(), clr.getGreen(), clr.getBlue());
        uChannelHistogram[yuvValues[1]]++;
        vChannelHistogram[yuvValues[2]]++;
    }
    private boolean isGreenChroma(int r, int g, int b) {
        int diff = Math.abs(r - GREEN_CHROMA_R) + Math.abs(g - GREEN_CHROMA_G) + Math.abs(b - GREEN_CHROMA_B);
        return diff < chroma_threshold;
    }

    /* HISTOGRAM COMPARISON PORTION */
    public double[][] normalizeHistogram(int[][] histogram) {
        int cols = histogram[0].length;
        double[] totals = new double[2];
        double[][] normalizedHistogram = new double[2][cols];

        // compute total sum for U and V histograms
        for (int j = 0; j < cols; j++) {
            totals[0] += histogram[0][j];
            totals[1] += histogram[1][j];
        }

        // check if totals are zero
        for (int i = 0; i < 2; i++) {
            if (totals[i] == 0) {
                System.out.println("Channel Histogram for  " + i + " null");
                return normalizedHistogram;  // Return the zeroed double array or handle the error appropriately
            }
        }

        // normalize each bin for U and V histograms
        for (int j = 0; j < cols; j++) {
            normalizedHistogram[0][j] = (double) histogram[0][j] / totals[0];
            normalizedHistogram[1][j] = (double) histogram[1][j] / totals[1];
        }

        return normalizedHistogram;
    }
    // compareNormalizedHistograms(): normalize, then compare
    public double compareNormalizedHistograms(int[][] inputImageHistogram, int[][] objectHistogram) {

        double[][] normalizedInput = normalizeHistogram(inputImageHistogram);
        double[][] normalizedObject = normalizeHistogram(objectHistogram);


        // Assuming both histograms have the same dimensions
        int rows = normalizedInput.length;
        int cols = normalizedInput[0].length;

        double res = 0.0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double a = normalizedInput[i][j];
                double b = normalizedObject[i][j];

                if (a + b == 0) { // Avoid division by zero
                    continue;
                }
                res += ((a - b) * (a - b)) / (a + b);
            }
        }

        return res / 2.0;
    }
}