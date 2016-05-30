/*
GeneticSolver.java
Nick Crews
4/7/16
*/

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;


public class GeneticSolver
{
    static Random rn = new Random();

    //All of the parameters for this solver
    public final int MAX_GENERATION;
    public final double MUTATION_RATE;
    public final int POPSIZE;
    public final int SAMPLE_SIZE;
    public final double PARENT_RATIO;
    public final double PERSIST_RATIO;

    // how many cities there are
    int n;
    // adjacency matix for this instances
    int[][] adj;
    // what generation number are we at?
    int generation;
    // the set of all paths
    List<Path> population;

    // Keep the list of nearest neighbors for each city, so that the NNA can just look them up
    Map<Integer, ArrayList<Integer>> nearest;

    public GeneticSolver(int[][] dist)
    {
        this.adj = dist;
        this.n = this.adj.length;
        this.POPSIZE = 100;
        this.MUTATION_RATE = .01;
        this.MAX_GENERATION = 50;
        this.PARENT_RATIO = .5;
        this.PERSIST_RATIO = .05;
        this.SAMPLE_SIZE = 2;

        // if we precompute the list of nearest neighbors for each city and cache it, it is way faster
        this.nearest = this.make_NNA_lookup();   
    }

    public void initialize()
    {
        this.population = init_population(this.POPSIZE);
        this.generation = 1;
    }

    public Path fittest_individual()
    {
        return Collections.min(this.population);
    }

    public ArrayList<Path> n_fittest(int n)
    {
        Collections.sort(this.population);
        ArrayList<Path> top = new ArrayList<Path>(this.population.subList(0, n));
        return top;
    }

    /*What is the average path length of this generation*/
    public int avg_fitness()
    {
        int total = 0;
        for (Path p: population)
        {
            total += p.length;
        }
        int avg = (int) Math.round(total / this.POPSIZE);
        return avg;

    }

    public int get_generation()
    {
        return this.generation;
    }

    /*Evolve the population one generation*/
    public void step()
    {
        // select individuals which will persist between generations, if they are the most fit
        int n_persisters = (int) Math.round(this.POPSIZE * this.PERSIST_RATIO);
        List<Path> persisters = n_fittest(n_persisters);

        // choose parents from our pool
        int n_parents = (int) Math.round(this.POPSIZE * this.PARENT_RATIO);
        List<Path> parents = tournament_select(population, n_parents);
        
        // breed these parents to result in popsize children
        int n_children = this.POPSIZE - n_persisters;
        List<Path>children = breed_population(parents, n_children);
        
        // make the population be these persisters and children
        this.population.clear();
        this.population.addAll(persisters);
        this.population.addAll(children);

        // mutate some of them
        mutate_population(population);
        
        this.generation++;
    }

    public boolean should_continue()
    {
        if (this.generation <= this.MAX_GENERATION)
        {
            return true;
        }
        return false;
    }

    /* generates popsize random starting paths, which are permutations of numbers 0 thru n-1*/
    private ArrayList<Path> init_population(int popsize)
    {
        // make our final result
        ArrayList<Path> pop = new ArrayList<Path>();
        // make an arraylist of the numbers 0 thru n-1, we will use this to shuffle and add later
        ArrayList<Integer> range = new ArrayList<>();
        for (int i=0; i<n; i++)
        {
            range.add(i);
        }

        // put random permutations into our population
        for (int i = 0; i < popsize; i++)
        {
            Collections.shuffle(range);
            pop.add(new Path(range));
        }

        return pop;
    }

    /*randomly choose SAMPLE_SIZE individuals from the population, choose the fittest, replace. Repeat until you have enough*/
    private ArrayList<Path> tournament_select(List<Path> population, int number)
    {
        // init the result
        ArrayList<Path> fittest = new ArrayList<Path>();

        ArrayList<Path> a_sample;
        while (fittest.size() < number)
        {
            a_sample = sample(population, this.SAMPLE_SIZE);
            fittest.add(Collections.min(a_sample));
        }

        return fittest;
    }

    /*Randomly sample number individuals from the population*/
    private ArrayList<Path> sample(List<Path> population, int number)
    {
        ArrayList<Path> result = new ArrayList<Path>();
        int random_index;
        for (int i = 0; i < number; i++)
        {
            random_index = rn.nextInt(population.size());
            result.add(population.get(random_index));
        }
        return result;
    }

    /* Uses the individuals in parents to create n_children children*/
    private ArrayList<Path> breed_population(List<Path> parents, int n_children)
    {
        // grow the list of parents into a pool large enough to breed n_children
        ArrayList<Path> pool = new ArrayList<Path>();
        // this is the best way I came up with for making sure no parent appears more than 1 more time than any other parent
        while (pool.size() < 2*n_children)
        {
            Collections.shuffle(parents);
            for (Path p: parents)
            {
                pool.add(p);
                if (pool.size() >= 2*n_children)
                {
                    break;
                }
            } 
        }

        // randomly choose 2 parents from pool without replacement and breed them
        ArrayList<Path> children = new ArrayList<Path>();
        Path p1, p2, child;
        Collections.shuffle(pool);
        for (int i = 0; i < pool.size()-1; i+=2)
        {
            p1 = pool.get(i);
            p2 = pool.get(i+1);
            child = breed(p1, p2);
            children.add(child);
        }
        
        return children;
    }

    private Path breed(Path p1, Path p2)
    {
        Graph g = new Graph();
        g.union(p1, p2);
        Path child = g.NNA();
        return child;
    }

    /*mutates a portion of the population*/
    private void mutate_population(List<Path> paths)
    {
        for (Path p: paths)
        {
            if (rn.nextDouble() < this.MUTATION_RATE)
            {
                p.mutate();
            } 
        }
    }

    /*Returns a map that lists the ordered list of nearest neighbors for each city*/
    private Map<Integer, ArrayList<Integer>> make_NNA_lookup()
    {
        Map<Integer, ArrayList<Integer>> result = new HashMap<Integer, ArrayList<Integer>>();

        // make a list of all the cities from 0 to n-1
        ArrayList<Integer> allCities = new ArrayList<Integer>();
        for (int i=0; i<this.n; i++)
        {
            allCities.add(i);
        }

        // go through all of the cities
        ArrayList<Integer> neighbors;
        for (int fromCity=0; fromCity < this.n; fromCity++)
        {
            // get all the neightbors to this city
            neighbors = new ArrayList<Integer>(allCities);
            neighbors.remove(fromCity);

            // now sort these neighbors by their distance to fromCity
            neighbors.sort( new NeighborComparator(fromCity) );

            // add these neighbors to the lookup table
            result.put(fromCity, neighbors);
        }
        return result;
    }

    /*Converts an ArrayList of Integers to an array on ints. WHY is there not a builtin method for this!?*/
    public static int[] convert(ArrayList<Integer> integerList) {
        int s = integerList.size();
        int[] intArray = new int[s];
        for (int i = 0; i < s; i++) {
            intArray[i] = integerList.get(i).intValue();
        }
        return intArray;
    }

    /*Used to compare the which of two neighbors is closer to a certain city*/
    private class NeighborComparator implements Comparator<Integer>
    {
        Integer from;

        public NeighborComparator(Integer fromCity)
        {
            this.from = fromCity;
        }

        @Override
        public int compare(Integer neighbor1, Integer neighbor2) 
        {
            return adj[this.from][neighbor1]-adj[this.from][neighbor2];
        }
    }

    /*Used to represent a graph, in order to implement the Nearest Neighbor Crossover*/
    private class Graph
    {
        int n;
        Map<Integer, HashSet<Integer>> edges;

        /* Make this graph be the union of the thwo paths*/
        public void union(Path p1, Path p2)
        {
            // init our n and edges
            this.n = p1.cities.length;
            this.edges = new HashMap<Integer, HashSet<Integer>>();
            for (int i = 0; i < this.n; i++)
            {
                this.edges.put(i, new HashSet<Integer>());
            }

            // populate our neighbor set
            int v1, v2;
            for (int i = 0; i < this.n; i++)
            {
                v1 = p1.cities[i];
                v2 = p1.cities[(i+1)%this.n];
                this.edges.get(v1).add(v2);
                this.edges.get(v2).add(v1);

                v1 = p2.cities[i];
                v2 = p2.cities[(i+1)%this.n];
                this.edges.get(v1).add(v2);
                this.edges.get(v2).add(v1);
            }
        }

        /*Start at a random city and try to complete a tour, greedily choosing the nearest neighbor in the union graph. Use edges from the complete graph if necessary*/
        public Path NNA() 
        {
            // init our result
            ArrayList<Integer> path = new ArrayList<Integer>();

            // choose a starting city
            Integer starting_city = rn.nextInt(n);
            path.add(starting_city);

            // set up our set of visited cities
            HashSet<Integer> visited = new HashSet<Integer>();
            visited.add(starting_city);

            
            // until the path is complete
            Integer prev_city;
            while(path.size() < this.n)
            {

                // get the last city in our partial tour
                prev_city = path.get(path.size()-1);

                // get all the connected cities, and sort them from closest to furthest
                ArrayList<Integer> candidates = new ArrayList<Integer> (edges.get(prev_city));
                candidates.sort( new NeighborComparator(prev_city) );

                // try to add them to the path
                boolean success = false;
                for (Integer neighbor: candidates)
                {
                    // make sure we haven't visited this city yet.
                    // otherwise, check the next closest city
                    if (!visited.contains(neighbor))
                    {
                        path.add(neighbor);
                        visited.add(neighbor);
                        success = true;
                        break;
                    }
                }
                // check to see if we succeeded in adding the neighbors from the union graph
                // if so, we don't need the next step
                if (success) {continue;}

                // if we couldn't add any of the neighbors in the union graph, resort to adding from the complete graph
                ArrayList<Integer> complete_graph_neighbors = nearest.get(prev_city);
                for (Integer neighbor: complete_graph_neighbors)
                {
                    if (!visited.contains(neighbor))
                    {
                        path.add(neighbor);
                        visited.add(neighbor);
                        break;
                    }
                }

            }

            // make a Path from our list of cities
            Path p = new Path(path);
            return p;
        }

        public String toString()
        {
            return this.edges.toString();
        }

    }

    /*Represents a tour. Specific to a GeneticSolver instance*/
    public class Path implements Comparable<Path>
    {
        public final int[] cities;
        public final int n_cities;
        public int length;

        public Path(int[] cities)
        {
            this.cities = cities;
            this.n_cities = cities.length;
            this.length = this.evaluate();

            assert this.isValid();
        }

        public Path(ArrayList<Integer> citiesAL)
        {
            this(convert(citiesAL));
        }

        /*computes the length of the path*/
        private int evaluate()
        {
            int score = 0;
            int from, to;
            for (int i = 0; i<this.n_cities-1; i++)
            {
                from = this.cities[i];
                to = this.cities[i+1];
                score += adj[from][to];
            }
            // add in the edge to return to first city
            int last_city = this.cities[this.n_cities-1];
            int first_city = this.cities[0];
            score += adj[last_city][first_city];
            return score;
        }

        /*for determining the natrual order of Paths, so they can be sorted easily*/
        public int compareTo(Path other)
        {
            return this.length-other.length;
        }

        /*mutate this path by reversing the subpath between 2 randomly selected cities*/
        public void mutate()
        {
            // get our two random cities
            int c1 = rn.nextInt(this.n_cities);
            int c2 = rn.nextInt(this.n_cities);

            // make sure the cities are in order. Also, we don't have deal with wraparound because paths are symmetrical
            int low = Math.min(c1,c2);
            int high = Math.max(c1,c2);

            // reverse the path
            int temp;
            for (int i = 0; i <(high-low+1)/2; i++)
            {
                temp = this.cities[low+i];
                this.cities[low+i] = this.cities[high-i];
                this.cities[high-i] = temp;
            }

            // update the length of this path
            this.length = this.evaluate();
        }

        public String toString()
        {
            return "Path{ cities: " + Arrays.toString(this.cities) + " length: " + String.format( "%d", this.length) + "}";
        }

        /*Makes sure this is a real path that contains the cities 0 through n-1*/
        public boolean isValid()
        {
            long total = 0;
            for (int city:this.cities)
            {
                total += Math.pow(2, city);
            }
            if (total == Math.pow(2, this.n_cities)-1)
            {
                return true;
            }
            return false;
        }
    }


}