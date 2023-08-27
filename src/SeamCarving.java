import java.util.ArrayList;

import java.util.Iterator;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*;
import java.awt.Color;

// Represents a image compression animation 
class SeamCarvingWorld extends World {
  SeamCarving image;
  Random rand;
  boolean highlight;
  SeamInfo current;
  boolean vertical;
  boolean pause;
  boolean grayScale;

  SeamCarvingWorld(String ref) {
    this.image = new SeamCarving(ref);
    this.rand = new Random();
    this.highlight = true;
    this.pause = false;
    this.grayScale = false;
    this.current = this.randomSeam();
  }

  // Draws the image
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(1000, 1000);
    scene.placeImageXY(this.image.drawFromPixel(this.grayScale), 500, 500);
    return scene;
  }

  // Each second remove a random seam from this image if we aren't paused
  public void onTick() {
    if (!this.pause) {
      if (this.highlight) {
        this.current = this.randomSeam();
        this.highlight = false;
        this.current.highlight(true);
      }
      else {
        this.image.removeSeam(this.current, this.vertical);
        this.highlight = true;
      }
    }
  }

  // Processes key presses
  public void onKeyEvent(String key) {
    // pauses the removal of random seams
    if (key.equals(" ")) {
      if (this.pause) {
        this.pause = false;
      }
      else {
        this.pause = true;
      }
    }
    // removes a vertical seam when the game is paused
    else if (key.equals("v") && this.pause) {
      this.current.highlight(false);
      this.image.removeSeam(this.image.findSeam(1, 0), true);
      this.highlight = true;
    }
    // removes a horizontal seam when the game is paused
    else if (key.equals("h") && this.pause) {
      this.current.highlight(false);
      this.image.removeSeam(this.image.findSeam(0, -1), false);
      this.highlight = true;
    }
    // toggles the grey scale based on pixel
    else if (key.equals("g")) {
      if (!this.grayScale) {
        this.grayScale = true;
      }
      else {
        this.grayScale = false;
      }
    }
  }

  // Returns a random seam either vertical or horizontal and sets the direction of
  // the seam we are removing
  SeamInfo randomSeam() {
    int random = this.rand.nextInt(2);
    if (random == 0) {
      this.vertical = true;
      return this.image.findSeam(1, 0);
    }
    else {
      this.vertical = false;
      return this.image.findSeam(0, -1);
    }
  }
}

// represents a image as pixels 
class SeamCarving {

  Pixel pixel;

  SeamCarving(String imageRef) {
    this.pixel = this.setPixel(new FromFileImage(imageRef));
  }

  // EFFECT: Initializes this pixel to contain all pixel information from the
  // image
  Pixel setPixel(FromFileImage image) {
    Pixel previous;
    Pixel current;
    ArrayList<ArrayList<Pixel>> pixels = new ArrayList<ArrayList<Pixel>>();
    ArrayList<Pixel> row = new ArrayList<Pixel>();
    for (int r = 0; r < image.getHeight(); r += 1) {
      // first pixel in the row
      current = new Pixel(image.getColorAt(0, r));
      row.add(current);
      if (r > 0) {
        current.up = pixels.get(r - 1).get(0);
        pixels.get(r - 1).get(0).down = current;
      }
      previous = current;
      for (int c = 1; c < image.getWidth(); c += 1) {
        current = new Pixel(image.getColorAt(c, r));
        current.left = previous;
        previous.right = current;
        if (r > 0) {
          current.up = pixels.get(r - 1).get(c);
          pixels.get(r - 1).get(c).down = current;
        }
        row.add(current);
        previous = current;
      }
      pixels.add(row);
      row = new ArrayList<Pixel>();
    }
    // return the top left pixel of the image
    return pixels.get(0).get(0);
  }

  // Returns a seam in the given direction (x = 1, y = 0 for vertical) (x = 0, y =
  // -1 for horizontal)
  SeamInfo findSeam(int x, int y) {
    Iterator<Pixel> iterator = new PixelIterator(this.pixel, x, y);
    // Create impossible seam to use for initial comparisons
    SeamInfo min = new SeamInfo(this.pixel, 1000, null);
    // Loop will end once there is no more pixel to be called in the iterator
    Pixel current;
    // will end once the iterator has no more pixels 
    while (iterator.hasNext()) {
      current = iterator.next();
      // if we are in the top row initialize seams
      if (current.direction(y, x) == current) {
        current.seam = new SeamInfo(current, current.getEnergy(), null);
      }
      else {
        current.setSeam(current.getMinSeam(x, y));
      }

      // if we are in the last row/column
      if (current.direction(-y, -x) == current && current.seam.compareTo(min) < 0) {
        min = current.seam;
      }
    }
    return min;
  }

  // EFFECT: Removes a given seam from this graph of pixels
  void removeSeam(SeamInfo seam, boolean vertical) {
    SeamInfo current = seam;
    // will end once we trace the seam back to null 
    while (current != null) {
      if (vertical) {
        current.current.remove(1, 0);
      }
      else {
        current.current.remove(0, -1);
      }
      // if we remove the top left pixel set this remove it from field variable
      if (current.current == this.pixel) {
        this.pixel = this.pixel.right;
      }
      current = current.cameFrom;
    }
  }

  // Returns this pixel represented as an image
  // determines if we want to display the pixel in gray scale or not
  WorldImage drawFromPixel(boolean isGreyScale) {
    ArrayList<ArrayList<Color>> pixels = new ArrayList<ArrayList<Color>>();
    ArrayList<Color> row = new ArrayList<Color>();
    Iterator<Pixel> iterator = this.pixel.iterator();
    Pixel current;
    Color currentColor;
    // add all the pixel colors to an array list
    // loop will terminate when the iterator has no more pixels 
    while (iterator.hasNext()) {
      current = iterator.next();
      float colorNum = (float) ((current.getEnergy() / 5));
      if (current.highlight) {
        currentColor = Color.red;
      }
      else if (isGreyScale) {
        currentColor = new Color(colorNum, colorNum, colorNum);
      }
      else {
        currentColor = current.color;
      }
      row.add(currentColor);
      if (current.right == current) {
        pixels.add(row);
        row = new ArrayList<Color>();
      }
    }

    // create an image based on dimensions of array list
    ComputedPixelImage image = new ComputedPixelImage(pixels.get(0).size(), pixels.size());
    // set all the pixels to be the correct colors
    for (int r = 0; r < pixels.size(); r += 1) {
      for (int c = 0; c < pixels.get(r).size(); c += 1) {
        image.setPixel(c, r, pixels.get(r).get(c));
      }
    }
    return image;
  }
}

// Represents a pixel in an image 
class Pixel implements Iterable<Pixel> {
  Pixel up;
  Pixel down;
  Pixel left;
  Pixel right;

  Color color;
  SeamInfo seam;
  boolean highlight;

  Pixel(Color color) {
    this.up = this;
    this.down = this;
    this.left = this;
    this.right = this;
    this.color = color;
  }

  // Returns an iterator that will iterate through each row of the graph
  public Iterator<Pixel> iterator() {
    return new PixelIterator(this, 1, 0);
  }

  // Returns the brightness of this pixel
  double getBrightness() {
    return ((this.color.getBlue() + this.color.getRed() + this.color.getGreen()) / 3.0) / 255.0;
  }

  // Returns the energy of this pixel
  double getEnergy() {
    double left = this.left.getBrightness();
    double right = this.right.getBrightness();
    double up = this.up.getBrightness();
    double down = this.down.getBrightness();
    double tL = this.up.left.getBrightness();
    double tR = this.up.right.getBrightness();
    double bL = this.down.left.getBrightness();
    double bR = this.down.right.getBrightness();
    // no bottom left, top left, or left pixel
    if (this.left == this) {
      tL = 0;
      bL = 0;
      left = 0;
    }
    // no bottom right, top right, or right pixel
    if (this.right == this) {
      tR = 0;
      bR = 0;
      right = 0;
    }
    // no up, top left, or top right pixel
    if (this.up == this) {
      up = 0;
      tR = 0;
      tL = 0;
    }
    // no down, bottom left, or bottom right pixel
    if (this.down == this) {
      down = 0;
      bL = 0;
      bR = 0;
    }
    // get the vertical and horizontal energy of this pixel
    double horizontal = (tL + (2 * left) + bL) - (tR + (2 * right) + bR);
    double vertical = (tL + (2 * up) + tR) - (bL + (2 * down) + bR);
    return Math.sqrt(Math.pow(horizontal, 2) + Math.pow(vertical, 2));
  }

  // EFFECT: Sets this seam based on information from previous seam passed in
  void setSeam(SeamInfo cameFrom) {
    this.seam = new SeamInfo(this, cameFrom.sum(this.getEnergy()), cameFrom);
  }

  // Returns the minimum seam of the 3 seams in given direction of this one
  // x = 1 y = 0 for finding the min seam above this pixel
  // x = 0 y = -1 for finding the min seam to the left of this pixel
  SeamInfo getMinSeam(int x, int y) {
    SeamInfo min = this.direction(y, x).direction(-x, y).seam;
    if (this.direction(y, x).seam.compareTo(min) < 0) {
      min = this.direction(y, x).seam;
    }
    if (this.direction(y, x).direction(x, -y).seam.compareTo(min) < 0) {
      min = this.direction(y, x).direction(x, -y).seam;
    }
    return min;
  }

  // Returns the pixel at given direction from this pixel
  Pixel direction(int x, int y) {
    if (x == 1 && y == 0) {
      return this.right;
    }
    else if (x == -1 && y == 0) {
      return this.left;
    }
    else if (x == 0 && y == 1) {
      return this.up;
    }
    else if (x == 0 && y == -1) {
      return this.down;
    }
    else {
      throw new RuntimeException("given direction is invalid");
    }
  }

  // EFFECT: Sets the pixel at given direction to give pixel
  void setDirection(Pixel pixel, int x, int y) {
    if (x == 1 && y == 0) {
      this.right = pixel;
    }
    else if (x == -1 && y == 0) {
      this.left = pixel;
    }
    else if (x == 0 && y == 1) {
      this.up = pixel;
    }
    else if (x == 0 && y == -1) {
      this.down = pixel;
    }
    else {
      throw new RuntimeException("given direction is invalid");
    }
  }

  // EFFECT: Removes this pixel from the graph based on given direction and fixes
  // structure of graph after removing
  // if we are removing horizontally x = 0 y = -1
  // if we are removing vertically x = 1 y = 0
  // when removing a pixel we have to move the pixels that come after it to the
  // removed pixels current position
  // if we remove vertically we move all the pixels to the right left one index
  // if we remove horizontally we move all the pixels below removed up one index
  void remove(int x, int y) {
    // treated as the pixel above the current when removing horizontally
    Pixel left = this.direction(-x, -y);
    // treated as the pixel below the current when removing horizontally
    Pixel right = this.direction(x, y);
    Pixel up = this.direction(y, x); // pixel to the left of current when removing horizontally
    // link past removed pixel
    left.setDirection(right, x, y);
    right.setDirection(left, -x, -y);
    // if we are at an edge
    if (left == this) {
      right.setDirection(right, -x, -y);
    }
    if (right == this) {
      left.setDirection(left, x, y);
    }
    Pixel removed = this.direction(-y, -x);
    Pixel current = this;
    // sets connections from upper row to this current row to account for removing a
    // pixel
    // loop terminates once we get to the last pixel in the row/column
    while (up.direction(x, y) != up) {
      // we aren't at the last row/column
      if (current.direction(y, x) != current) {
        // links this pixel to the new pixel "below" it, shifts all pixels after the
        // removed pixel to proper spots
        up.setDirection(right, -y, -x);
        // links the pixel "above" this current pixel to itself
        right.direction(y, x).setDirection(right.direction(y, x), -y, -x);
        // pixel we are shifting over's "above" pixel to the one above the current pixel
        right.setDirection(up, y, x);
      }

      // if we aren't in the last row/column
      if (current.direction(-y, -x) != current) {
        // accounts for children pixels that get lost when removing a pixel in a higher
        // row/column
        // maintains the removed pixels children position in the image
        right.setDirection(removed, -y, -x);
        removed.setDirection(right, y, x);
      }
      // accumulates to the next pixel in current row/column to fix structure
      current = current.direction(x, y);
      removed = removed.direction(x, y);
      up = up.direction(x, y);
      right = right.direction(x, y);
    }
  }

}

// Represents a seam info, used to remove a seam from an image 
class SeamInfo implements Comparable<SeamInfo> {
  Pixel current;
  double totalWeight;
  SeamInfo cameFrom;

  SeamInfo(Pixel current, double totalWeight, SeamInfo cameFrom) {
    this.current = current;
    this.totalWeight = totalWeight;
    this.cameFrom = cameFrom;
  }

  // Determines if this seam is greater than, less than, or equal to other seam
  // based on their total weight
  public int compareTo(SeamInfo s) {
    return (int) (this.totalWeight - s.totalWeight);
  }

  // Returns the sum of this seams total weight and given energy
  double sum(double energy) {
    return this.totalWeight + energy;
  }

  // EFFECT: Highlights all the pixels in this seam
  void highlight(boolean highlight) {
    SeamInfo temp = this;
    while (temp != null) {
      temp.current.highlight = highlight;
      temp = temp.cameFrom;
    }
  }

}

// Represents an iterator to loop through a pixel 
class PixelIterator implements Iterator<Pixel> {

  boolean keepGoing;
  Pixel pixel;
  Pixel firstInRow;
  // direction we are traversing in
  // x = 1, y = 0 for row traversal
  // x = 0, y = -1 for column traversal
  int x;
  int y;

  PixelIterator(Pixel pixel, int x, int y) {
    this.keepGoing = true;
    this.pixel = pixel;
    this.firstInRow = pixel;
    this.x = x;
    this.y = y;
  }

  // Checks if this pixel has a next pixel
  public boolean hasNext() {
    return this.keepGoing;
  }

  // Returns the next pixel of this pixel
  public Pixel next() {
    if (!this.keepGoing) {
      throw new RuntimeException("no more pixels to iterate through");
    }
    // pixel in given direction of the current one
    Pixel next = this.pixel.direction(x, y);
    Pixel current = this.pixel;
    // we are at the last pixel (bottom right)
    if (current.down == current && current.right == current) {
      this.keepGoing = false;
    }
    // we are at the end of a row/column
    else if (current.direction(x, y) == current) {
      this.firstInRow = this.firstInRow.direction(-y, -x);
      next = this.firstInRow;
    }
    this.pixel = next;
    return current;
  }

}

// From part 1 we abstracted the code to iterate over our graph using an iterator
// while we don't explicitly create a border pixel we account for border pixels 
// being initialized to themselves efficiently through our method implementations 
// therefore we create efficient code that is easy to read and well adapted to the 
// structure of the problem 
class ExamplesSeamCarving {

  ComputedPixelImage twoByTwo = new ComputedPixelImage(2, 2);
  ComputedPixelImage threeByThree = new ComputedPixelImage(3, 3);
  ComputedPixelImage threeByTwo = new ComputedPixelImage(3, 2);
  ComputedPixelImage twoByThree = new ComputedPixelImage(2, 3);
  ComputedPixelImage threeByThree2 = new ComputedPixelImage(3, 3);

  ComputedPixelImage twoByOneSeamRemoved = new ComputedPixelImage(1, 2);
  ComputedPixelImage oneByTwoHorizSeamRemoved = new ComputedPixelImage(2, 1);
  ComputedPixelImage threeByThreeBlackDiagonalLine = new ComputedPixelImage(3, 3);
  ComputedPixelImage threeByThreeRemoved = new ComputedPixelImage(2, 3);
  ComputedPixelImage threeByThreeHorizRemoved = new ComputedPixelImage(3, 2);
  ComputedPixelImage twoByThreeRemoved = new ComputedPixelImage(1, 3);
  ComputedPixelImage threeByTwoRemoved = new ComputedPixelImage(2, 2);
  ComputedPixelImage twoByThreeHorizRemoved = new ComputedPixelImage(2, 2);
  ComputedPixelImage threeByTwoHorizRemoved = new ComputedPixelImage(3, 1);
  ComputedPixelImage threeByThree2Removed = new ComputedPixelImage(1, 3);
  ComputedPixelImage threeByThree2HorizRemoved = new ComputedPixelImage(3, 1);

  SeamCarving threeByThreeBlackDiagonalLinePixels;
  SeamCarving twoByOneSeamRemovedPixels;
  SeamCarving oneByTwoHorizSeamRemovedPixels;
  SeamCarving twoByThreeRemovedPixels;
  SeamCarving twoByThreeHorizRemovedPixels;
  SeamCarving threeByTwoHorizRemovedPixels;
  SeamCarving threeByThree2HorizRemovedPixels;

  SeamCarving twoByTwoPixels;
  SeamCarving threeByThreePixels;
  SeamCarving threeByTwoPixels;
  SeamCarving twoByThreePixels;
  SeamCarving threeByThree2Pixels;
  SeamCarving threeByThree2RemovedPixels;

  SeamCarving threeByThreeHorizRemovedPixels;
  SeamCarving threeByThreeRemovedSeam;
  SeamCarving threeByTwoRemovedPixels;

  void initialize() {
    this.twoByTwo.setPixel(0, 0, Color.blue);
    this.twoByTwo.setPixel(0, 1, Color.black);
    this.twoByTwo.setPixel(1, 0, Color.red);
    this.twoByTwo.setPixel(1, 1, Color.green);
    this.twoByTwo.saveImage("TwoByTwo.png");

    this.threeByThree.setPixel(0, 0, Color.black);
    this.threeByThree.setPixel(0, 1, Color.blue);
    this.threeByThree.setPixel(0, 2, Color.red);
    this.threeByThree.setPixel(1, 0, Color.green);
    this.threeByThree.setPixel(2, 0, Color.yellow);
    this.threeByThree.setPixel(1, 1, Color.orange);
    this.threeByThree.setPixel(2, 2, Color.magenta);
    this.threeByThree.setPixel(1, 2, Color.cyan);
    this.threeByThree.setPixel(2, 1, Color.white);
    this.threeByThree.saveImage("ThreeByThree.png");

    this.threeByThree2.setPixel(0, 0, Color.black);
    this.threeByThree2.setPixel(0, 1, Color.blue);
    this.threeByThree2.setPixel(0, 2, Color.red);
    this.threeByThree2.setPixel(1, 0, Color.green);
    this.threeByThree2.setPixel(2, 0, Color.yellow);
    this.threeByThree2.setPixel(1, 1, Color.orange);
    this.threeByThree2.setPixel(2, 2, Color.magenta);
    this.threeByThree2.setPixel(1, 2, Color.cyan);
    this.threeByThree2.setPixel(2, 1, Color.white);
    this.threeByThree2.saveImage("threeByThree2.png");

    this.threeByTwo.setPixel(0, 0, Color.black);
    this.threeByTwo.setPixel(0, 1, Color.blue);
    this.threeByTwo.setPixel(1, 0, Color.green);
    this.threeByTwo.setPixel(1, 1, Color.red);
    this.threeByTwo.setPixel(2, 0, Color.yellow);
    this.threeByTwo.setPixel(2, 1, Color.orange);
    this.threeByTwo.saveImage("ThreeByTwo.png");

    this.twoByThree.setPixel(0, 0, Color.black);
    this.twoByThree.setPixel(0, 1, Color.blue);
    this.twoByThree.setPixel(1, 0, Color.green);
    this.twoByThree.setPixel(1, 1, Color.red);
    this.twoByThree.setPixel(0, 2, Color.yellow);
    this.twoByThree.setPixel(1, 2, Color.orange);
    this.twoByThree.saveImage("TwoByThree.png");

    this.threeByThreeBlackDiagonalLine.setPixel(0, 0, Color.black);
    this.threeByThreeBlackDiagonalLine.setPixel(1, 0, Color.white);
    this.threeByThreeBlackDiagonalLine.setPixel(2, 0, Color.white);
    this.threeByThreeBlackDiagonalLine.setPixel(0, 1, Color.white);
    this.threeByThreeBlackDiagonalLine.setPixel(1, 1, Color.black);
    this.threeByThreeBlackDiagonalLine.setPixel(2, 1, Color.white);
    this.threeByThreeBlackDiagonalLine.setPixel(0, 2, Color.white);
    this.threeByThreeBlackDiagonalLine.setPixel(1, 2, Color.white);
    this.threeByThreeBlackDiagonalLine.setPixel(2, 2, Color.black);
    this.threeByThreeBlackDiagonalLine.saveImage("threeByThreeBlackDiagonalLinePixels.png");

    this.twoByOneSeamRemoved.setPixel(0, 0, Color.red);
    this.twoByOneSeamRemoved.setPixel(0, 1, Color.green);
    this.twoByOneSeamRemoved.saveImage("twoByOneSeamRemoved.png");

    this.oneByTwoHorizSeamRemoved.setPixel(0, 0, Color.blue);
    this.oneByTwoHorizSeamRemoved.setPixel(1, 0, Color.green);
    this.oneByTwoHorizSeamRemoved.saveImage("twoByOneHorizSeamRemoved.png");

    this.threeByThreeRemoved.setPixel(0, 0, Color.green);
    this.threeByThreeRemoved.setPixel(1, 0, Color.yellow);
    this.threeByThreeRemoved.setPixel(0, 1, Color.orange);
    this.threeByThreeRemoved.setPixel(1, 1, Color.white);
    this.threeByThreeRemoved.setPixel(0, 2, Color.cyan);
    this.threeByThreeRemoved.setPixel(1, 2, Color.magenta);
    this.threeByThreeRemoved.saveImage("ThreeByThreeRemovedSeam.png");

    this.threeByThree2Removed.setPixel(0, 0, Color.yellow);
    this.threeByThree2Removed.setPixel(0, 1, Color.orange);
    this.threeByThree2Removed.setPixel(0, 2, Color.magenta);
    this.threeByThree2Removed.saveImage("threeByThree2Removed.png");

    this.threeByThreeHorizRemoved.setPixel(0, 0, Color.black);
    this.threeByThreeHorizRemoved.setPixel(1, 0, Color.green);
    this.threeByThreeHorizRemoved.setPixel(2, 0, Color.white);
    this.threeByThreeHorizRemoved.setPixel(0, 1, Color.blue);
    this.threeByThreeHorizRemoved.setPixel(1, 1, Color.cyan);
    this.threeByThreeHorizRemoved.setPixel(2, 1, Color.magenta);
    this.threeByThreeHorizRemoved.saveImage("threeByThreeHorizRemoved.png");

    this.threeByThree2HorizRemoved.setPixel(0, 0, Color.black);
    this.threeByThree2HorizRemoved.setPixel(1, 0, Color.green);
    this.threeByThree2HorizRemoved.setPixel(2, 0, Color.magenta);
    this.threeByThree2HorizRemoved.saveImage("threeByThree2HorizRemoved.png");

    this.twoByThreeRemoved.setPixel(0, 0, Color.green);
    this.twoByThreeRemoved.setPixel(0, 1, Color.red);
    this.twoByThreeRemoved.setPixel(0, 2, Color.orange);
    this.twoByThreeRemoved.saveImage("twoByThreeRemoved.png");

    this.threeByTwoRemoved.setPixel(0, 0, Color.green);
    this.threeByTwoRemoved.setPixel(1, 0, Color.yellow);
    this.threeByTwoRemoved.setPixel(0, 1, Color.red);
    this.threeByTwoRemoved.setPixel(1, 1, Color.orange);
    this.threeByTwoRemoved.saveImage("threeByTwoRemoved.png");

    this.twoByThreeHorizRemoved.setPixel(0, 0, Color.black);
    this.twoByThreeHorizRemoved.setPixel(1, 0, Color.red);
    this.twoByThreeHorizRemoved.setPixel(0, 1, Color.yellow);
    this.twoByThreeHorizRemoved.setPixel(1, 1, Color.orange);
    this.twoByThreeHorizRemoved.saveImage("twoByThreeHorizRemoved.png");

    this.threeByTwoHorizRemoved.setPixel(0, 0, Color.black);
    this.threeByTwoHorizRemoved.setPixel(1, 0, Color.green);
    this.threeByTwoHorizRemoved.setPixel(2, 0, Color.orange);
    this.threeByTwoHorizRemoved.saveImage("threeByTwoHorizRemoved.png");

    this.twoByTwoPixels = new SeamCarving("TwoByTwo.png");
    this.threeByThreePixels = new SeamCarving("ThreeByThree.png");
    this.threeByThree2Pixels = new SeamCarving("threeByThree2.png");
    this.threeByTwoPixels = new SeamCarving("ThreeByTwo.png");
    this.twoByThreePixels = new SeamCarving("TwoByThree.png");
    this.threeByThreeBlackDiagonalLinePixels = new SeamCarving(
        "threeByThreeBlackDiagonalLinePixels.png");
    this.twoByOneSeamRemovedPixels = new SeamCarving("twoByOneSeamRemoved.png");
    this.threeByThreeRemovedSeam = new SeamCarving("ThreeByThreeRemovedSeam.png");
    this.threeByThree2RemovedPixels = new SeamCarving("threeByThree2Removed.png");
    this.oneByTwoHorizSeamRemovedPixels = new SeamCarving("twoByOneHorizSeamRemoved.png");
    this.threeByThreeHorizRemovedPixels = new SeamCarving("threeByThreeHorizRemoved.png");
    this.twoByThreeRemovedPixels = new SeamCarving("twoByThreeRemoved.png");
    this.threeByTwoRemovedPixels = new SeamCarving("threeByTwoRemoved.png");
    this.twoByThreeHorizRemovedPixels = new SeamCarving("twoByThreeHorizRemoved.png");
    this.threeByThree2HorizRemovedPixels = new SeamCarving("threeByThree2HorizRemoved.png");
  }
  // imp world methods were test visually

  void testHighlightInSeamInfo(Tester t) {
    this.initialize();
    SeamInfo seam = this.threeByThreePixels.findSeam(1, 0);
    Pixel pixel = this.threeByThreePixels.pixel;
    seam.highlight(true);
    // check if each pixel in the seam gets highlighted
    t.checkExpect(seam.current.highlight, true);
    t.checkExpect(seam.cameFrom.current.highlight, true);
    t.checkExpect(seam.cameFrom.cameFrom.current.highlight, true);
  }

  boolean testSetSeam(Tester t) {
    this.initialize();

    Pixel testPix = new Pixel(Color.red);

    SeamInfo seamInfoThree = new SeamInfo(testPix, 3, null);
    SeamInfo seamInfoOne = new SeamInfo(testPix, 1, null);

    this.threeByThreePixels.pixel.setSeam(seamInfoThree);
    this.twoByTwoPixels.pixel.setSeam(seamInfoOne);

    return t.checkInexact(this.threeByThreePixels.pixel.seam.totalWeight, 4.783, 0.001)
        && t.checkExpect(this.threeByThreePixels.pixel.seam.cameFrom, seamInfoThree)
        && t.checkExpect(this.threeByThreePixels.pixel.seam.current, this.threeByThreePixels.pixel)

        && t.checkInexact(this.twoByTwoPixels.pixel.seam.totalWeight, 2.054, 0.001)
        && t.checkExpect(this.twoByTwoPixels.pixel.seam.cameFrom, seamInfoOne)
        && t.checkExpect(this.twoByTwoPixels.pixel.seam.current, this.twoByTwoPixels.pixel);
  }

  void testGetMinSeam(Tester t) {
    // 2 X 3 Pixel grid with all pixels connected
    Pixel topRow1 = new Pixel(Color.black);
    Pixel topRow2 = new Pixel(Color.red);
    Pixel topRow3 = new Pixel(Color.blue);
    Pixel bottomRow1 = new Pixel(Color.magenta);
    Pixel bottomRow2 = new Pixel(Color.cyan);
    Pixel bottomRow3 = new Pixel(Color.gray);
    // Connect all the pixels properly
    topRow1.right = topRow2;
    topRow1.down = bottomRow1;
    topRow1.seam = new SeamInfo(topRow1, 3, null);
    topRow2.left = topRow1;
    topRow2.right = topRow3;
    topRow2.down = bottomRow2;
    topRow2.seam = new SeamInfo(topRow2, 5, null);
    topRow3.left = topRow2;
    topRow3.down = bottomRow3;
    topRow3.seam = new SeamInfo(topRow3, 7, null);
    bottomRow1.up = topRow1;
    bottomRow1.right = bottomRow2;
    bottomRow1.seam = new SeamInfo(bottomRow1, 2, null);
    bottomRow2.up = topRow2;
    bottomRow2.left = bottomRow1;
    bottomRow2.right = bottomRow3;
    bottomRow3.left = bottomRow2;
    bottomRow3.up = topRow3;
    // finding the min seam above this pixel for each pixel in the bottom row,
    // checks all 3 pixels above each one
    t.checkExpect(bottomRow1.getMinSeam(1, 0), topRow1.seam);
    t.checkExpect(bottomRow2.getMinSeam(1, 0), topRow1.seam);
    t.checkExpect(bottomRow3.getMinSeam(1, 0), topRow2.seam);
    // finding the min seam for all pixels in the second column, checks all the
    // seams to the 3 pixels to the left of each one
    t.checkExpect(topRow2.getMinSeam(0, -1), bottomRow1.seam);
    t.checkExpect(bottomRow2.getMinSeam(0, -1), bottomRow1.seam);
  }

  boolean testDirection(Tester t) {
    this.initialize();

    return t.checkExpect(this.threeByTwoPixels.pixel.direction(1, 0),
        this.threeByTwoPixels.pixel.right)
        && t.checkExpect(this.threeByTwoPixels.pixel.direction(-1, 0),
            this.threeByTwoPixels.pixel.left)
        && t.checkExpect(this.threeByTwoPixels.pixel.direction(0, 1),
            this.threeByTwoPixels.pixel.up)
        && t.checkExpect(this.threeByTwoPixels.pixel.direction(0, -1),
            this.threeByTwoPixels.pixel.down);
  }

  boolean testSetDirection(Tester t) {
    this.initialize();

    Pixel testPixRight = new Pixel(Color.blue);
    this.twoByTwoPixels.pixel.setDirection(testPixRight, 1, 0);

    Pixel testPixLeft = new Pixel(Color.blue);
    this.threeByTwoPixels.pixel.setDirection(testPixLeft, -1, 0);

    Pixel testPixUp = new Pixel(Color.blue);
    this.twoByThreePixels.pixel.setDirection(testPixUp, 0, 1);

    Pixel testPixDown = new Pixel(Color.blue);
    this.threeByThreePixels.pixel.setDirection(testPixDown, 0, -1);

    return t.checkExpect(this.twoByTwoPixels.pixel.right, testPixRight)
        && t.checkExpect(this.threeByTwoPixels.pixel.left, testPixLeft)
        && t.checkExpect(this.twoByThreePixels.pixel.up, testPixUp)
        && t.checkExpect(this.threeByThreePixels.pixel.down, testPixDown);
  }

  boolean testRemove(Tester t) {
    this.initialize();

    // if we are removing horizontally x = 0 y = -1
    // if we are removing vertically x = 1 y = 0
    // top middle pixel
    Color newThreeByThreePixel = Color.green;
    // remove top left
    this.threeByThreePixels.pixel.remove(1, 0);

    // top right pixel
    Color newTwoByTwoPixel = Color.blue;

    // remove bottom left pixel
    this.twoByTwoPixels.pixel.down.remove(0, -1);

    // top right pixel
    Color newThreeByTwoPixel = Color.yellow;

    // remove middle top pixel
    this.threeByTwoPixels.pixel.right.remove(1, 0);

    // middle left pixel
    Color newTwoByThreePixels = Color.blue;

    // remove bottom left pixel
    this.twoByThreePixels.pixel.down.down.remove(0, -1);

    // first test is to check that top left pixel has been removed since remove
    // doesnt change where the pixel points but just cuts it out of the grid
    return t.checkExpect(newThreeByThreePixel, this.threeByThreePixels.pixel.right.left.color)
        && t.checkExpect(newTwoByTwoPixel, this.twoByTwoPixels.pixel.down.color)
        && t.checkExpect(newThreeByTwoPixel, this.threeByTwoPixels.pixel.right.color)
        && t.checkExpect(newTwoByThreePixels, this.twoByThreePixels.pixel.down.down.color);
  }

  boolean testSum(Tester t) {
    SeamInfo seamInfoThousand = new SeamInfo(new Pixel(Color.red), 1000, null);
    SeamInfo seamInfoOne = new SeamInfo(new Pixel(Color.red), 1, null);
    SeamInfo seamInfoThree = new SeamInfo(new Pixel(Color.red), 3, null);

    return t.checkExpect(seamInfoThousand.sum(2.0), 1002.0)
        && t.checkExpect(seamInfoOne.sum(0.0), 1.0) && t.checkExpect(seamInfoThree.sum(1.5), 4.5);
  }

  boolean testCompareTo(Tester t) {
    SeamInfo seamInfoThousand = new SeamInfo(new Pixel(Color.red), 1000, null);
    SeamInfo seamInfoOne = new SeamInfo(new Pixel(Color.red), 1, null);

    SeamInfo seamInfoThree = new SeamInfo(new Pixel(Color.red), 3, null);
    SeamInfo seamInfoTwo = new SeamInfo(new Pixel(Color.red), 2, null);

    return t.checkExpect(seamInfoThousand.compareTo(seamInfoOne), 999)
        && t.checkExpect(seamInfoTwo.compareTo(seamInfoThree), -1);
  }

  boolean testSetPixel(Tester t) {
    this.initialize();

    // general checks that diagonal references and right left down up references
    // point to same things
    return t.checkExpect(this.twoByTwoPixels.pixel.right.down, twoByTwoPixels.pixel.down.right)
        && t.checkExpect(this.twoByTwoPixels.pixel.right.right, twoByTwoPixels.pixel.right)
        && t.checkExpect(this.threeByThreePixels.pixel.right.down,
            threeByThreePixels.pixel.down.right)
        && t.checkExpect(this.twoByThreePixels.pixel.right.right.down,
            twoByThreePixels.pixel.down.right.right)
        && t.checkExpect(this.twoByThreePixels.pixel.right.color, Color.green)
        && t.checkExpect(this.twoByThreePixels.pixel.down.down.color, Color.yellow)
        && t.checkExpect(this.threeByTwoPixels.pixel.down.right.right,
            threeByTwoPixels.pixel.right.right.down)
        && t.checkExpect(this.threeByTwoPixels.pixel.right.down, threeByTwoPixels.pixel.down.right)
        && t.checkExpect(this.twoByTwoPixels.pixel.color, Color.blue)
        && t.checkExpect(this.twoByTwoPixels.pixel.down.color, Color.black)
        && t.checkExpect(this.twoByTwoPixels.pixel.right.color, Color.red);
  }

  void testIterator(Tester t) {
    this.initialize();
    Pixel pixel = this.threeByThreePixels.pixel;
    Iterator<Pixel> iterator = pixel.iterator();
    t.checkExpect(iterator.hasNext(), true);
    t.checkExpect(iterator.next(), pixel);
    t.checkExpect(iterator.next(), pixel.right);
    t.checkExpect(iterator.next(), pixel.right.right);
    // at the end of the first row check if iterator moves down
    t.checkExpect(iterator.next(), pixel.down);
    iterator.next();
    iterator.next();
    iterator.next();
    iterator.next();
    // last pixel in the graph
    t.checkExpect(iterator.next(), pixel.down.down.right.right);
    t.checkExpect(iterator.hasNext(), false);
    t.checkException(new RuntimeException("no more pixels to iterate through"), iterator, "next");
  }

  boolean testDrawFromPixel(Tester t) {
    this.initialize();
    return t.checkExpect(this.twoByTwoPixels.drawFromPixel(false), this.twoByTwo)
        && t.checkExpect(this.threeByThreePixels.drawFromPixel(false), this.threeByThree)
        && t.checkExpect(this.twoByThreePixels.drawFromPixel(false), this.twoByThree)
        && t.checkExpect(this.threeByTwoPixels.drawFromPixel(false), this.threeByTwo);
  }

  boolean testGetBrightness(Tester t) {
    this.initialize();

    return t.checkInexact(this.twoByTwoPixels.pixel.getBrightness(), 0.33333, 0.001)
        && t.checkInexact(this.twoByTwoPixels.pixel.down.getBrightness(), 0.0000, 0.001)
        && t.checkInexact(this.threeByThreePixels.pixel.right.down.down.getBrightness(), 0.66666,
            0.001)
        && t.checkInexact(this.threeByThreePixels.pixel.down.right.getBrightness(), 0.5947712,
            0.001);
  }

  boolean testGetEnergy(Tester t) {
    this.initialize();
    // testing top left corner case
    return t.checkInexact(this.threeByThreePixels.pixel.getEnergy(), 1.781, 0.01)
        // check top right corner case
        && t.checkInexact(this.threeByThreePixels.pixel.right.getEnergy(), 3.219, 0.01)
        // check middle case
        && t.checkInexact(this.threeByThreePixels.pixel.down.right.getEnergy(), 2.531, 0.01)
        // check bottom left corner case
        && t.checkInexact(this.threeByThreePixels.pixel.down.down.getEnergy(), 2.288, 0.01)
        // check bottom right corner case
        && t.checkInexact(this.threeByThreePixels.pixel.down.down.right.right.getEnergy(), 3.223,
            0.01)
        // check middle right case
        && t.checkInexact(this.threeByThreePixels.pixel.down.right.right.getEnergy(), 2.202, 0.01)
        // check middle left case
        && t.checkInexact(this.threeByThreePixels.pixel.down.getEnergy(), 2.392, 0.01)
        // check top middle case
        && t.checkInexact(this.threeByThreePixels.pixel.right.right.getEnergy(), 2.885, 0.01)
        // check bottom middle case
        && t.checkInexact(this.threeByThreePixels.pixel.down.down.right.getEnergy(), 2.847, 0.01);
  }

  boolean testFindSeam(Tester t) {
    this.initialize();
    SeamInfo threeByThreeVertical = this.threeByThreePixels.findSeam(1, 0);
    SeamInfo threeByThreeHorizontalSeam = this.threeByThreePixels.findSeam(0, -1);
    Pixel threeByThreePixel = this.threeByThreePixels.pixel;

    SeamInfo twoByTwoVertical = this.twoByTwoPixels.findSeam(1, 0);
    SeamInfo twoByTwoPixelsHorizontalSeam = this.twoByTwoPixels.findSeam(0, -1);
    Pixel twoByTwoPixel = this.twoByTwoPixels.pixel;
    // we expect the seam to be the first column of the three by three image
    // check the first pixel in the seam to be the bottom right pixel
    return t.checkExpect(threeByThreeVertical.current, threeByThreePixel.down.down)
        // check the second pixel in the seam to be the middle of the 1st column
        && t.checkExpect(threeByThreeVertical.cameFrom.current, threeByThreePixel.down)
        // check the third to be the top left pixel
        && t.checkExpect(threeByThreeVertical.cameFrom.cameFrom.current, threeByThreePixel)
        && t.checkExpect(threeByThreeVertical.cameFrom.cameFrom.cameFrom, null)

        // first pixel to be bottom of first column
        && t.checkExpect(threeByThreeHorizontalSeam.current, threeByThreePixel.down.down)
        // second pixel to be 2nd col 2nd row
        && t.checkExpect(threeByThreeHorizontalSeam.cameFrom.current, threeByThreePixel.down.right)
        // third pixel to be 1st col 3rd row
        && t.checkExpect(threeByThreeHorizontalSeam.cameFrom.cameFrom.current,
            threeByThreePixel.right.right)
        && t.checkExpect(threeByThreeHorizontalSeam.cameFrom.cameFrom.cameFrom, null)

        // first pixel is bottom left
        && t.checkExpect(twoByTwoVertical.current, twoByTwoPixel.right)
        // second pixel is top left
        && t.checkExpect(twoByTwoVertical.cameFrom.current, twoByTwoPixel)
        && t.checkExpect(twoByTwoVertical.cameFrom.cameFrom, null)

        // first pixel is bottom left
        && t.checkExpect(twoByTwoPixelsHorizontalSeam.current, twoByTwoPixel.down)
        // second pixel is top right
        && t.checkExpect(twoByTwoPixelsHorizontalSeam.cameFrom.current, twoByTwoPixel.right)
        && t.checkExpect(twoByTwoPixelsHorizontalSeam.cameFrom.cameFrom, null);
  }

  boolean testRemoveVerticalSeam(Tester t) {
    this.initialize();
    SeamInfo seamTwoByTwo = this.twoByTwoPixels.findSeam(1, 0);
    this.twoByTwoPixels.removeSeam(seamTwoByTwo, true);

    SeamInfo seamThreeByThree = this.threeByThreePixels.findSeam(1, 0);
    this.threeByThreePixels.removeSeam(seamThreeByThree, true);

    SeamInfo seamTwoByThree = this.twoByThreePixels.findSeam(1, 0);
    this.twoByThreePixels.removeSeam(seamTwoByThree, true);

    SeamInfo seamThreeByTwo = this.threeByTwoPixels.findSeam(1, 0);
    this.threeByTwoPixels.removeSeam(seamThreeByTwo, true);

    SeamInfo seamThreeByThree2 = this.threeByThree2Pixels.findSeam(1, 0);
    this.threeByThree2Pixels.removeSeam(seamThreeByThree2, true);
    SeamInfo seamThreeByThree21 = this.threeByThree2Pixels.findSeam(1, 0);
    this.threeByThree2Pixels.removeSeam(seamThreeByThree21, true);

    // included wellformednesschecks as well
    // tests removing a seam from a 2x2
    return t.checkExpect(this.twoByTwoPixels.drawFromPixel(false), this.twoByOneSeamRemoved)
        && t.checkExpect(this.twoByTwoPixels.pixel.down.right, this.twoByTwoPixels.pixel.right.down)
        // tests removing seam from a 3x3
        && t.checkExpect(this.threeByThreePixels.drawFromPixel(false), this.threeByThreeRemoved)
        && t.checkExpect(this.threeByThreePixels.pixel.down.right,
            this.threeByThreePixels.pixel.right.down)
        // tests checking a 2x3
        && t.checkExpect(this.twoByThreePixels.drawFromPixel(false), this.twoByThreeRemoved)
        && t.checkExpect(this.twoByThreePixels.pixel.down.right,
            this.twoByThreePixels.pixel.right.down)
        // tests checking a 3x2
        && t.checkExpect(this.threeByTwoPixels.drawFromPixel(false), this.threeByTwoRemoved)
        && t.checkExpect(this.threeByTwoPixels.pixel.down.right,
            this.threeByTwoPixels.pixel.right.down)
        // test checking that removing 2 seams from a 3x3 works
        && t.checkExpect(this.threeByThree2Pixels.drawFromPixel(false), this.threeByThree2Removed)
        && t.checkExpect(this.threeByThree2Pixels.pixel.down.right,
            this.threeByThreePixels.pixel.right.down);
  }

  boolean testRemoveHorizontalSeam(Tester t) {
    this.initialize();

    SeamInfo seamTwoByTwo = this.twoByTwoPixels.findSeam(0, -1);
    this.twoByTwoPixels.removeSeam(seamTwoByTwo, false);

    SeamInfo seamThreeByThree = this.threeByThreePixels.findSeam(0, -1);
    this.threeByThreePixels.removeSeam(seamThreeByThree, false);

    SeamInfo seamTwoByThree = this.twoByThreePixels.findSeam(0, -1);
    this.twoByThreePixels.removeSeam(seamTwoByThree, false);

    SeamInfo seamThreeByTwo = this.threeByTwoPixels.findSeam(0, -1);
    this.threeByTwoPixels.removeSeam(seamThreeByTwo, false);

    SeamInfo seamThreeByThree2 = this.threeByThree2Pixels.findSeam(0, -1);
    this.threeByThree2Pixels.removeSeam(seamThreeByThree2, false);
    SeamInfo seamThreeByThree21 = this.threeByThree2Pixels.findSeam(0, -1);
    this.threeByThree2Pixels.removeSeam(seamThreeByThree21, false);

    // included wellformednesschecks as well
    // 3x2 and 2x3 too
    // tests removing a seam from a 2x2
    return t.checkExpect(this.twoByTwoPixels.drawFromPixel(false), this.oneByTwoHorizSeamRemoved)
        && t.checkExpect(this.twoByTwoPixels.pixel.down.right, this.twoByTwoPixels.pixel.right.down)
        // tests removing seam from a 3x3
        && t.checkExpect(this.threeByThreePixels.drawFromPixel(false),
            this.threeByThreeHorizRemoved)
        && t.checkExpect(this.threeByThreePixels.pixel.down.right,
            this.threeByThreePixels.pixel.right.down)
        // tests removing seam from 2x3
        && t.checkExpect(this.twoByThreePixels.drawFromPixel(false), this.twoByThreeHorizRemoved)
        && t.checkExpect(this.twoByThreePixels.pixel.down.right,
            this.twoByThreePixels.pixel.right.down)
        // tests removing seam from 3x2
        && t.checkExpect(this.threeByTwoPixels.drawFromPixel(false), this.threeByTwoHorizRemoved)
        && t.checkExpect(this.threeByTwoPixels.pixel.down.right,
            this.threeByTwoPixels.pixel.right.down)
        // test checking that removing 2 seams from a 3x3 works
        && t.checkExpect(this.threeByThree2Pixels.drawFromPixel(false),
            this.threeByThree2HorizRemoved)
        && t.checkExpect(this.threeByThree2Pixels.pixel.down.right,
            this.threeByThreePixels.pixel.right.down);
  }

  void testBigBang(Tester t) {
    SeamCarvingWorld world = new SeamCarvingWorld("balloons.png");
    world.bigBang(1000, 1000, 0.001);
  }

}
