/**
 * Defines a SeamCarver object by a picture from which the seams with the
 * lowest energy pixels will be removed or to which low-energy seams will
 * be added.
 * 
 * Remarks:
 * - TODO: image expansion not implemented
 */

import java.awt.Color;
import java.util.Arrays;

/**
 * Provides methods and member variables to remove and add seams. This class
 * is defined by a Picture object and its width and height. The methods to help
 * with resizing include ones that calculate energies, find and remove seams 
 * horizontally, and rotate the picture to perform the same seam finding
 * algorithm in the vertical direction
 * 
 * @author Gabriel Alzate
 */
public class SeamCarver {

    private Picture picture;    //the picture from which to remove seams
    private int width, height;  //width and height of picture

    private boolean debug;      //activates debug messages

    /**
     * SeamCarver ctor from a picture
     * 
     * @param picture the picture the SeamCarver will be based on
     */
    public SeamCarver(Picture picture){
        this.picture = picture;
        this.width = picture.width();
        this.height = picture.height();
        this.debug = false;
    }

    /**
     * Picture accessor method
     * 
     * @return the picture field variable
     */
    public Picture getPicture(){
        return picture;
    }

    /**
     * Picture width accessor method
     * 
     * @return the width field variable
     */
    public int getWidth(){
        return width;
    }

    /**
     * Picture height accessor method
     * 
     * @return the height field variable
     */
    public int getHeight(){
        return height;
    }

    /**
     * Rotates picture clockwise to find vertical seam so that horizontal
     * seam functions can simply be used on a flipped picture
     * 
     * @return the rotated picture
     */
    private Picture rotateClockwise(){

        //blank canvas with the same dimensions as the picture
        Picture transposed = new Picture(height, width);

        //rotate
        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                transposed.setRGB(height - 1 - j, i, picture.getRGB(i, j));

        //change member variables
        int temp = height;
        height = width;
        width = temp;

        return transposed; 
    }

    /**
     * Reverse the effect of rotate clockwise
     * 
     * @return the rotated picture
     */
    private Picture rotateCtrClockwise(){

        //blank canvas with the same dimensions as the picture
        Picture transposed = new Picture(height, width);

        //rotate
        for(int i = 0; i < width; i++)
            for(int j = 0; j < height; j++)
                transposed.setRGB(j, width - 1 - i, picture.getRGB(i, j));

        //change member variables
        int temp = height;
        height = width;
        width = temp;

        return transposed; 
    }

    /**
     * Calculates the energy of pixel at (x, y)
     * 
     * @param x x-coordinate of the pixel
     * @param y y-coordinate of the pixel
     * 
     * @return the energy of the pixel
     */
    public double energy(int x, int y){

        //check for invalid x, y
        try{
            if(x < 0 || x > width || y < 0 || y > height)
                throw new IndexOutOfBoundsException();
        } catch (IndexOutOfBoundsException e){
            e.printStackTrace();
            System.out.println("Coordinates out of bounds!");
            return -1.0;
        }

        //calculate border pixels considering edge cases
        int leftX = (x != 0) ? x - 1 : width - 1;
        int rightX = (x != width - 1) ? x + 1 : 0;
        int topY = (y != 0) ? y - 1 : height - 1;
        int bottomY = (y != height - 1) ? y + 1 : 0;

        //delta squares to calculate energy
        double deltaXSq = Math.pow( ((picture.getRGB(rightX, y) >> 16) & 0xff) - ((picture.getRGB(leftX, y) >> 16) & 0xff), 2 ) +
                            Math.pow( ((picture.getRGB(rightX, y) >> 8) & 0xff) - ((picture.getRGB(leftX, y) >> 8) & 0xff), 2 ) +
                            Math.pow( (picture.getRGB(rightX, y) & 0xff) - (picture.getRGB(leftX, y) & 0xff), 2 );

        double deltaYSq = Math.pow( ((picture.getRGB(x, topY) >> 16) & 0xff) - ((picture.getRGB(x, bottomY) >> 16) & 0xff), 2 ) +
                            Math.pow( ((picture.getRGB(x, topY) >> 8) & 0xff) - ((picture.getRGB(x, bottomY) >> 8) & 0xff), 2 ) +
                            Math.pow( (picture.getRGB(x, topY) & 0xff) - (picture.getRGB(x, bottomY) & 0xff), 2 );

        return Math.sqrt(deltaXSq + deltaYSq);

    }

    /**
     * Helper method to fill energy matrix.
     * 
     * @return the energy matrix
     */
    private double[][] calculateEnergies(){

        //parallel matrix to hold energy values of each pixel
        double[][] energyVals = new double[width][height];

        //loop to get energy values
        for(int row = 0; row < width; row++)
            for(int col =  0; col < height; col++)
                energyVals[row][col] = energy(row, col);

        //debug message to print energy matrix
        if(debug){
            System.err.println("ENERGY MATRIX:\n");
            for(int i = 0; i < energyVals.length; i++)
                System.err.println(Arrays.toString(energyVals[i]));
        }

        return energyVals;

    }

    /**
     * Finds a horizontal seam with the least energy using dynamic programming
     * through an energy matrix
     * 
     * @return the least energetic pixel seam
     */
    public int[] findHSeam(){

        //seam with least energies
        int[] hSeam = new int[width];

        //energy values
        double[][] energyVals = calculateEnergies();

        //adjacent pixels in previous column
        int backPre, backPost;

        //temp variable to store min value
        double min;

        //calculate costs from row 2 -> end
        for(int row = 1; row < width; row++){
            for(int col = 0; col < height; col++){
                
                //the pixels in the column before that are adjacent
                backPre = (col != 0) ? col - 1 : 0;
                backPost = (col != height - 1) ? col + 1 : height - 1;

                //add current energy to lowest energy above it to dynamically calculate seam path
                energyVals[row][col] += Math.min(
                                        Math.min(energyVals[row - 1][backPre], energyVals[row - 1][backPost]),
                                        energyVals[row - 1][col]);

            }
        }

        //bounds of the seam (adjacent)
        int start = 0;
        int end = height - 1;

        //backtrace the lowest costs
        for(int row = width - 1; row >= 0; row--){

            min = Double.MAX_VALUE;

            for(int col = end; col >= start; col--){

                //find lowest energy in each column
                if(energyVals[row][col] <= min){
                    min = energyVals[row][col];
                    hSeam[row] = col;
                }

            }

            //change the bounds to make sure seam is acyclic
            start = (hSeam[row] != 0) ? hSeam[row] - 1 : 0;
            end = (hSeam[row] != height - 1) ? hSeam[row] + 1 : height - 1;

        }

        //debug message to see the seam's values
        if(debug)
            System.err.println(Arrays.toString(hSeam));

        return hSeam;

    }

    /**
     * Finds vertical seam by implementing findHSeam on a rotated picture
     * 
     * @return the least energetic pixel seam
     */
    public int[] findVSeam(){

        //delegate to findHSeam on a transposed picture
        picture = rotateClockwise();
        int[] vSeam = findHSeam();

        //reverse order to undo the backward effects of transposing
        for(int i = 0; i < vSeam.length/2; i++){
            int temp = vSeam[i];
            vSeam[i] = vSeam[vSeam.length - 1 - i];
            vSeam[vSeam.length - 1 - i] = temp;
        }

        //rotate picture back upright
        picture = rotateCtrClockwise();

        return vSeam;

    }

    /**
     * Removes a horizontal seam from the picture
     * 
     * @param seam the seam consisting of x-values of pixels with the lowest
     *             energies
     */
    public void removeHSeam(int[] seam){

        //new canvas with less width onto which the seamed picture is copied
        Picture edited = new Picture(width, --height);

        //copy pixels to new pic
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){

                //copy the next row's pixels after the seam value is passed
                if(j >= seam[i])
                    edited.setRGB(i, j, picture.getRGB(i, j + 1));
                else
                    edited.setRGB(i, j, picture.getRGB(i, j));

            }
        }

        //reset the picture
        picture = edited;

    }

    /**
     * Removes a vertical seam from the picture
     * 
     * @param seam the seam consisting of y-values of pixels with the lowest
     *             energies
     */
    public void removeVSeam(int[] seam){

        //new canvas with less height onto which the seamed picture is copied
        Picture edited = new Picture(--width, height);

        //copy pixels to new pic
        for(int j = 0; j < height; j++){
            for(int i = 0; i < width; i++){

                //copy the next column's pixels after the seam value is passed
                if(i >= seam[j])
                    edited.setRGB(i, j, picture.getRGB(i + 1, j));
                else
                    edited.setRGB(i, j, picture.getRGB(i, j));

            }
        }

        //reset the picture
        picture = edited;

    } 

}
