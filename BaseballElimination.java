/**
 *
 * @author Webber Huang
 */

import java.util.List;
import java.util.ArrayList;

public class BaseballElimination {
    
    private final short num;
    private final List<String> teams;
    private final short[] w, l, r;
    private final short[][] g;
    private List<Integer> R;
    private boolean isEliminatedExec;
    
    // create a baseball division from given filename in format specified below
    public BaseballElimination(String filename) {
        In in = new In(filename);
        
        // initial variable
        num = in.readShort();
        teams = new ArrayList<>();
        w = new short[num];
        l = new short[num];
        r = new short[num];
        g = new short[num][num];
        
        int i = 0;
        while (!in.isEmpty()) {
            teams.add(in.readString());
            w[i] = in.readShort();
            l[i] = in.readShort();
            r[i] = in.readShort();
            for (int j = 0; j < num; j++) 
                g[i][j] = in.readShort();            
            i++;
        }
    }   
    
    // number of teams
    public int numberOfTeams() {
        return num;
    }   
    
    // all teams
    public Iterable<String> teams() {
        return teams;
    }  
    
    // number of wins for given team
    public int wins(String team) {
        int i = checkTeam(team);
        return w[i];
    }     
    
    // number of losses for given team
    public int losses(String team) {
        int i = checkTeam(team);
        return l[i];
    }   
    
    // number of remaining games for given team
    public int remaining(String team) {
        int i = checkTeam(team);
        return r[i];
    } 
    
    // number of remaining games between team1 and team2
    public int against(String team1, String team2) {
        int i = checkTeam(team1);
        int j = checkTeam(team2);
        return g[i][j];
    }   
    
    // is given team eliminated?
    public boolean isEliminated(String team) {
        int x = checkTeam(team);  
        isEliminatedExec = true;
        return computeR(x);
    }  
    
    // subset R of teams that eliminates given team; null if not eliminated
    public Iterable<String> certificateOfElimination(String team) {
        int x = checkTeam(team);
        if (!isEliminatedExec) computeR(x);
        
        isEliminatedExec = false;
        if (R.isEmpty()) return null;
        
        List<String> result = new ArrayList<>();
        for (int i : R)
            result.add(teams.get(i));
        return result;
    } 
    
    /**
     * Helper methods
     */
    private boolean computeR(int x) {
        R = new ArrayList<>();
        
        // trivial eliminated solution
        for (int i = 0; i < num; i++)
            if (i != x && w[x] + r[x] < w[i]) {
                R.add(i);
                return true;
            }             
        
        // nontrivial eliminated solution
        int count = num*num + num + 2;
        FlowNetwork fn = buildFlowNetwork(x, count);
        
        // compute min cut with ford-fulkerson method
        FordFulkerson FF = new FordFulkerson(fn, count-2, count-1);
        
        for (int i = 0; i < num; i++) 
            if (FF.inCut(i)) R.add(i);
        
        return w[x] + r[x] < averageR(x);
    }
    
    private FlowNetwork buildFlowNetwork(int x, int count) {
        boolean[][] gMarked = new boolean[num][num];        
        FlowNetwork fn = new FlowNetwork(count);
        
        // connect team vertices to t
        for (int i = 0; i < num; i++)
            if (i != x) fn.addEdge(new FlowEdge(i, count-1, w[x] + r[x] - w[i]));
        
        // connect s to game vertices and game vertices to team vertices
        for (int i = num; i < num*num + num; i++) {
            int m = (i - num) % num;
            int n = (i - num) / num;
            int cap = g[m][n];
            if (x != m && x != n && m != n && !gMarked[n][m]) {
                fn.addEdge(new FlowEdge(count-2, i, cap));
                fn.addEdge(new FlowEdge(i, m, Double.POSITIVE_INFINITY));
                fn.addEdge(new FlowEdge(i, n, Double.POSITIVE_INFINITY));
                gMarked[m][n] = true;
            }
        }             
        return fn;
    }
    
    private int checkTeam(String team) {
        if (!teams.contains(team))
            throw new java.lang.IllegalArgumentException("Invalid Team:" + team);
        return teams.indexOf(team);
    }
    
    private double averageR(int x) {
        int wr = 0, gr = 0;
        boolean[][] gMarked = new boolean[num][num];
        for (int i : R) {
            wr += w[i];
            for (int j = 0; j < num; j++) 
                if (i != j && j != x && !gMarked[j][i]) {
                    gr += g[i][j];
                    gMarked[i][j] = true;            
                }
        }
        return (double) (wr + gr) / R.size();
    }

    public static void main(String[] args) {        
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                StdOut.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team))
                    StdOut.print(t + " ");
                StdOut.println("}");
            }
            else {
                StdOut.println(team + " is not eliminated");
            }
        }
    }    
}
