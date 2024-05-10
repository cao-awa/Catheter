# Use matrix

First, need build a matrix format like:

```java
class Test {
    public static void simpleMatrix() {
        // The width is 4.
        LongCatheter.make(
                1, 2, 3, 4,
                5, 6, 7, 8
        ).matrixEach(4, (pos, item) -> {
            // The 'x' is the row of matrix.
            System.out.println(pos.x());
            // The 'y' is the line of matrix.
            System.out.println(pos.y());

            // For example:
            // [
            //   1, 2, 3, 4,
            //   5, 6, 7, 8
            // ]
            // If x is 1 and y is 0,
            // That means the pos is at the number '2'.
            // Using index to read, y is 0 means at line 1,
            // Then x is 1 means at row 2,

            // The 'item' is the element at this pos.
            System.out.println(item);
        });
    }
}
```

## Calculate matrix

Simple summing compute:

```java
class Test {
    public static void simpleComputeMatrix() {
        // Want to compute matrix, need two matrix first.

        // Source is the first matrix in the compute.
        LongCatheter source = LongCatheter.make(
                1, 2, 3, 4,
                5, 6, 7, 8
        );

        // Input is the second matrix in the compute.
        LongCatheter input = LongCatheter.make(
                1, 1, 1, 1,
                1, 1, 1, 1
        );

        // Method 'matrixHomoVary' make matrix become a new matrix.
        // That doing point to point compute, this is why it required homogeneous matrix. 
        source.matrixHomoVary(4, input, (pos, sourceX, inputX) -> {
            // Simplest calculate, just add it.
            return sourceX + inputX;
        });

        // After computing, 'source' should see likes: 
        // [
        //   2, 3, 4, 5
        //   6, 7, 8, 9
        // ]
        // Because it element was summing from another matrix where it all is 1.
    }
}
```

Maybe you want do more advanced computing, you can use two non-homogeneous matrix to computing:

```java
class Test {
    public static void simpleComputeMatrix() {
        // The non-homogeneous matrix computing must follow a rule:
        // Let we define the express of matrix is A(h, w), h is height, w is width.
        // And 'A' is source matrix, define a 'B' as input matrix.
        // The matrix A and B look likes: A(h, w):B(h, w).
        // When we want computing these matrix,
        // Must make sure the matrix is A(h, x):B(x, w) .
        // This expression explained, the width of B must equals to height of A.
        // Otherwise you cannot do compute using these two matrix.
        //
        // When we computing matrix, will create a new matrix,
        // This matrix is sized as A(w)*B(h).
        // So if we have 2x4 source and 4x2 input(in this example),
        // The computing will product a new 2x2 matrix.  
        
        // Source is the first matrix in the compute.
        LongCatheter source = LongCatheter.make(
                1, 2, 3, 4,
                5, 6, 7, 8
        );

        // Input is the second matrix in the compute.
        LongCatheter input = LongCatheter.make(
                1, 5,
                2, 6,
                3, 7,
                4, 8
        ); 

        // Method 'matrixHomoVary' make matrix become a new matrix.
        // That doing point to point compute, this is why it required homogeneous matrix. 
        source.matrixMap(4, 2, input, (flockPos, sourcePos, inputPos, sourceX, inputX) -> {
            // Let we do multiplication.
            return sourceX * inputX;
        }, (destPos, combine1, combine2) -> {
            // In matrix multiplication, a result is the sum of the whole row multiplication results.
            // For example, we computing the result at new matrix pos (0<x>, 0<y>),
            // Then we should process: 
            // The last results of
            //   - Source (0,0) meet Input (0,0)
            //   - Source (1,0) meet Input (0,1)
            //   - Source (2,0) meet Input (0,2)
            //   - Source (3,0) meet Input (0,3)
            // If computing result at matrix pos (0<x>, 1<y>), then should process:
            // The last results of
            //   - Source (0,0) meet Input (1,0)
            //   - Source (1,0) meet Input (1,1)
            //   - Source (2,0) meet Input (1,2)
            //   - Source (3,0) meet Input (1,3)
            // If computing result at matrix pos (1<x>, 1<y>), then should process:
            // The last results of
            //   - Source (0,1) meet Input (1,0)
            //   - Source (1,1) meet Input (1,1)
            //   - Source (2,1) meet Input (1,2)
            //   - Source (3,1) meet Input (1,3)
            // Other similar, simply summarize is:
            // The last results of
            //   - Source (a,x) meet Input (y,a)
            //   - Source (a+1,x) meet Input (y,a+1)
            //   - Source (a+2,x) meet Input (y,a+2)
            //   - ......
            //   - Source (a+n,x) meet Input (y,a+n)
            // The 'x' is result matrix 'x' index, 'y' same too. edge is the size of result matrix.
            // The 'a' is a range starting from zero, edge is width of source matrix (or height of input matrix) 
            // Result matrix computing these matrix has indexed by A(w) and B(h), because it is produced from these two parameters.
            return combine1 + combine2;
        });

        // After computing, 'result' should see likes: 
        // [
        //   30, 70
        //   70, 174
        // ]
    }
}
```