# TetraCluster
A parallel program in Java using the Parallel Java 2 Library. The programs runs on a cluster parallel computer to find the smallest tetrahedron from the given set of points

Smallest Tetrahedron


A tetrahedron is a solid with four corners and four triangular faces. A tetrahedron is specified by the 3-D coordinates of its corners: (x1, y1, z1), (x2, y2, z2), (x3, y3, z3), and (x4, y4, z4). The volume V of a tetrahedron is given by this formula, where "abs" is the absolute value:

Volume = abs [(x1 y2 z3 − x1 y2 z4 − x1 y3 z2 + x1 y3 z4 + x1 y4 z2 − x1 y4 z3 − x2 y1 z3 + x2 y1 z4 + x2 y3 z1 − x2 y3 z4 − x2 y4 z1 + x2 y4 z3 + x3 y1 z2 − x3 y1 z4 − x3 y2 z1 + x3 y2 z4 + x3 y4 z1 − x3 y4 z2 − x4 y1 z2 + x4 y1 z3 + x4 y2 z1 − x4 y2 z3 − x4 y3 z1 + x4 y3 z2) / 6]

Given a list of 3-D points, every quadruplet of four different points defines a different tetrahedron. In a list with n points, there are choose(n,4) = n(n − 1)(n − 2)(n − 3)/24 quadruplets. We want to find the quadruplet that yields the tetrahedron with the smallest volume.

The given program will run in a parallel environment to find the smallest tetrahedron. The parallel program is designed to run on a cluster of multicore nodes.

Program Input and Output

The program's command line argument is the name of an input file. Each line of the input file consists of three double precision floating point numbers (type double) separated by whitespace, that specify the X, Y, and Z coordinates of one 3-D point. The points are indexed starting at 0; that is, the first line is the point at index 0, the second line is the point at index 1, the third line is the point at index 2, and so on. The input file must contain at least four points.

The program prints the smallest tetrahedron found. The first output line gives the first corner of the smallest tetrahedron. The second output line gives the second corner of the smallest tetrahedron. The third output line gives the third corner of the smallest tetrahedron. The fourth output line gives the fourth corner of the smallest tetrahedron. Each of the first four lines consists of:

The index of the corner, an integer.
A space character.
A left parenthesis.
The X coordinate of the corner, a double precision floating point number.
A comma.
The Y coordinate of the corner, a double precision floating point number.
A comma.
The Z coordinate of the corner, a double precision floating point number.
A right parenthesis.
The four corners must be printed in ascending order of their indexes. The fifth output line gives the volume of the smallest tetrahedron, a double precision floating point number.

For further references regarding parallel java library and how to use it, please refer to: https://www.cs.rit.edu/~ark/pj2.shtml

Utkarsh Bhatia
