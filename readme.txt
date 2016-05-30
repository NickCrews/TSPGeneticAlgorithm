Uses a genetic algorithm to estimate a solution to the Travelling Salesman Problem. Based off of the Nearest Neighbor Crossover algorithm described in "An Evolutionary Approach to the TSP" by Sural et al 2010, DOI 10.1007/978-3-642-10701-6_15.

This repository contains the 3 source files, plus the compiled bytecode, this readme, a folder containing problem files, and another folder containing solution files.

I used problem instances from TSPLIB (http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/). These instances are classic benchmarks, and many of them have verified optimal solutions, so that performance can be compared. I wanted to be able to visually display the problem, so I just used the EUC_2D instances that repesent actual locations in the plane, so the cities could be easily drawn.

To run the demo, you need to specify one of the problem files to use, and you can optionally specify a solution file, so that the optimum tour is overlaid on the display as well. Some problem files have no solution file, that's just how TSPLIB works, in that case give it the argument 'NONE'.

example usage:
java TSP problems/tsp225.tsp solutions/tsp225.opt.tour
java TSP problems/tsp225.tsp NONE
java TSP problems/bier127.tsp NONE

Contact: ncrewsak@gmail.com
More abou this project can be found at http://nickcrews.weebly.com/genetic-algorithm-for-tsp.html
