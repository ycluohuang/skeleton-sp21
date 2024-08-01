package timingtest;
//package algs4;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeAList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeAListConstruction();
    }

    public static void timeAListConstruction() {
        // TODO: YOUR CODE HERE
//        int[] times = {1000, 2000, 4000, 6000, 8000, 16000, 32000, 64000, 128000};
        AList<Integer> Ns = new AList<>();
        AList<Double> times = new AList<>();
        AList<Integer> Op = new AList<>();
        for(int i = 1000; i <= 128000; i *= 2) {
            AList<Integer> AL = new AList<>();
            Stopwatch sw = new Stopwatch();
            for(int j = 0; j < i; j += 1) {AL.addLast(j);}
            double time = sw.elapsedTime();
            Ns.addLast(i);
            times.addLast(time);
            Op.addLast(i);
        }
        printTimingTable(Ns, times, Op);
    }
}
