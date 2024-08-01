package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
  // YOUR TESTS HERE
    @Test
    public void testThreeAddThreeRemove(){
        AListNoResizing<Integer> AL = new AListNoResizing<>();
        BuggyAList<Integer> BL = new BuggyAList<>();
        int[] test = {4, 5, 6};
        for(int i : test){
            AL.addLast(i);
            BL.addLast(i);
        }
        for(int i = 0; i < test.length; i++){
           assertEquals(AL.removeLast(), BL.removeLast());
        }
        assertEquals(AL.size(), BL.size());
    }

    @Test
    public void randomizedTest(){
        AListNoResizing<Integer> L = new AListNoResizing<>();
        BuggyAList<Integer> BL = new BuggyAList<>();
        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                BL.addLast(randVal);
//                System.out.println("addLast(" + randVal + ")");
            } else if (operationNumber == 1) {
                // size
                int size = L.size();
                int sizeBl = BL.size();
//                System.out.println("L.size: " + size);
//                System.out.println("BL.size: " + sizeBl);
                assertEquals(size, sizeBl);
            }
            else if(L.size() != 0 && operationNumber == 2) {
                int tmp = L.getLast();
                int tmpBl = BL.getLast();
//                System.out.println("getLast: " + tmp);
//                System.out.println("getLast: " + tmpBl);
                assertEquals(tmp, tmpBl);
            }
            else if(L.size() != 0 && operationNumber == 3) {
                int del_L = L.removeLast();
                int del_BL = BL.removeLast();
                assertEquals(del_L, del_BL);
            }
        }

    }

}
