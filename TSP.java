/*
TSP.java
Nick Crews
4/7/16
*/
import java.lang.Thread;

import java.io.File;
import java.util.Scanner;
import java.util.Arrays;
import java.util.ArrayList;

public class TSP
{
    
    public static void main(String[] args)
    {

        if (args.length != 2)
        {
            System.out.println("usage: java TSP <problemfile (.tsp file)> <solutionfile (.opt.tour file), or NONE>");
            System.exit(1);
        }

        // read the options
        String problem_file_path = args[0];
        String solution_file_path = args[1];

        // load the list of cities
        int[][] cities = load_cities(problem_file_path);
        int[][] adjacency = compute_distances(cities);

        // set up our solver
        GeneticSolver solver = new GeneticSolver(adjacency);

        // get the name of the problem, which is between the last / and the first '.' Heinous, I know
        //e.g. data/berlin52.tsp is problem berlin52
        String[] tokens = problem_file_path.split("\\.");
        tokens = tokens[0].split("/");
        String instance_name = tokens[tokens.length-1];

        // make our Plotter for visualizing what's going on
        Plotter plotter = new Plotter("TSPLIB Instance " + instance_name);
        plotter.set_cities(cities);

        // display the final solution if there is one
        if (!solution_file_path.equals("NONE"))
        {
            ArrayList<Integer> optimal_cities = load_optimal_tour(solution_file_path);
            GeneticSolver.Path optimal = solver.new Path(optimal_cities);
            plotter.add_optimal_path(optimal);
        }

        
        // make it so we can pause and wait for the user to type something
        Scanner in = new Scanner(System.in);
        System.out.println("Enter anything to start the simulation, enter 'q' any time to exit");
        if (in.nextLine().toLowerCase().equals("q"))
        {
            System.exit(0);
        }
       
        
        GeneticSolver.Path best = null;
        int average_fitness;
        int generation;
        solver.initialize();
        while (solver.should_continue())
        {
            average_fitness = solver.avg_fitness();
            generation = solver.get_generation();

            best = solver.fittest_individual();
            plotter.show_path(best, generation);

            // wait for the user before evolving to next generation
            System.out.print("Generation " + generation + "/" + solver.MAX_GENERATION + " had average length " + average_fitness);
            System.out.print(". Enter anything to continue, or 'q' to exit: ");
            if (in.nextLine().toLowerCase().equals("q"))
            {
                System.exit(0);
            }

            // EVOLVE!
            solver.step();
        }

        System.out.println("The best solution found was: " + best);
        System.out.println("Enter anything to exit");
        in.nextLine();
        System.exit(0);
    }

    /*Make an array that holds [x, y] coords for the cities*/
    public static int[][] load_cities(String problem_file_path)
    {
        // create our Scanner Object
        Scanner s;
        try
        {
            s = new Scanner(new File(problem_file_path));    
        }
        catch (Exception e)
        {
            System.out.println(e);
            System.exit(1);
            return null;
        }

        // read the metadata
        int n = 0;
        while (true)
        {
            String line = s.nextLine().trim();
            if (line.startsWith("DIMENSION"))
            {
                n = Integer.parseInt(line.split(":")[1].trim());
            }
            if (line.startsWith("EDGE_WEIGHT_TYPE"))
            {
                if (!line.split(":")[1].trim().equals("EUC_2D"))
                {
                    System.out.println("input file must be of type 'EUC_2D'");
                    System.exit(1);
                }
            }
            if (line.startsWith("NODE_COORD_SECTION"))
            {
                break;
            }
        }

        // read the actual data
        int[][] cities = new int[n][2];
        while (s.hasNextLine())
        {
            String line = s.nextLine().trim();
            if (line.startsWith("EOF") || line.equals(""))
            {
                break;
            }

            String[] tokens = line.split(" ");
            int i = Integer.parseInt(tokens[0])-1;
            int x = (int) Math.round(Double.parseDouble(tokens[1]));
            int y = (int) Math.round(Double.parseDouble(tokens[2]));

            cities[i] = new int[] {x, y};
        }

        return cities;
    }

    /*Create the list of cities which is the optimal tour for this problem*/
    public static ArrayList<Integer> load_optimal_tour(String filepath)
    {
        // create our Scanner Object
        Scanner s;
        try
        {
            s = new Scanner(new File(filepath));    
        }
        catch (Exception e)
        {
            System.out.println(e);
            System.exit(1);
            return null;
        }

        while (!s.nextLine().trim().equals("TOUR_SECTION"))
        {
            continue;
        }

        // read the actual data
        ArrayList<Integer> cities = new ArrayList<Integer>();
        while (s.hasNextLine())
        {
            String line = s.nextLine().trim();
            if (line.startsWith("EOF") || line.equals("") || line.equals("-1"))
            {
                break;
            }
            String[] tokens = line.split(" ");
            for (String token: tokens)
            {
                String trimmed = token.trim();
                if (trimmed.equals(""))
                {
                    continue;
                }
                int city = Integer.parseInt(trimmed)-1;
                cities.add(city); 
            }
            
        }

        return cities;

    }

    /* makes an adjacency matrix for a list of cities with certain coordinates*/
    public static int[][] compute_distances(int[][] cities)
    {
        int n = cities.length;
        int [][] adjacency = new int[n][n];
        for (int from = 0; from < n; from++)
        {
            int fromx = cities[from][0];
            int fromy = cities[from][1];
            for (int to = 0; to < n; to++)
            {
                int tox = cities[to][0];
                int toy = cities[to][1];
                adjacency[from][to] = distance(fromx, fromy, tox, toy);
            }
        }
        return adjacency;
    }

    /* To follow conventions of TSPLIB, we round all distances to integers*/
    private static int distance(int x1, int y1, int x2, int y2)
    {
        return (int) Math.round(Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)));
    }


}