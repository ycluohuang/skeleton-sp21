package deque;

import afu.org.checkerframework.checker.oigj.qual.O;
import org.apache.commons.collections.iterators.ArrayIterator;
import org.junit.*;

import java.util.Comparator;
import java.util.Iterator;


public class ArrayDeque<T> implements Deque<T>,Iterable<T> {

    private T[] items;
    private int size, nextFirst, nextLast; // First是头部插入, Last是尾部插入

    private void initIndex(int newNextFirst, int newNextLast) {
        nextFirst = newNextFirst;
        nextLast = newNextLast;
    }

    public ArrayDeque() {
        items = (T[]) new Object[8];
        size = 0;
        initIndex(3, 4);
    }

    public int getFirstIndex(){
        return (nextFirst + 1)% items.length;
    }

    public int getLastIndex(){
        return (nextLast - 1 + items.length)% items.length;
    }

    private int updateIndexBackward(int item){
        return (item + 1)%items.length;
    }

    private int updateIndexForward(int item){
        return (item - 1 + items.length)% items.length;
    }

    private void resize(int capacity) {
        T[] newItems = (T[]) new Object[capacity];
        int firstIndex = getFirstIndex();
        int lastIndex = getLastIndex();

        if (firstIndex <= lastIndex) {
            for (int i = firstIndex, j = 0; i <= lastIndex; i++, j++) {
                newItems[j] = items[i];
            }
        } else {
            int cnt = 0;
            for (int i = firstIndex; i < items.length; i++, cnt++) {
                newItems[cnt] = items[i];
            }
            for (int i = 0; i <= lastIndex; i++, cnt++) {
                newItems[cnt] = items[i];
            }
        }

        items = newItems;
        nextFirst = capacity - 1;
        nextLast = size;
    }
    @Override
    public void addFirst(T item){
        if(size == items.length){
            resize(2 * size);
        }

        items[nextFirst] = item;
        nextFirst = updateIndexForward(nextFirst);
        size += 1;
    }

    @Override
    public void addLast(T item){
        if(size == items.length){
            resize(2 * size);
        }

        items[nextLast] = item;
        nextLast = updateIndexBackward(nextLast);
        size += 1;
    }

    @Override
    public int size(){
        return size;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    @Override
    public void printDeque(){
        for(int i = 0;i < size;i++){
            System.out.print(items[i] + " ");
        }
        System.out.println();
    }

    @Override
    public T removeFirst(){
        if(size == 0) return null;

        int index = getFirstIndex();
        T item = items[index];
        items[index] = null;
        size -= 1;
        nextFirst = updateIndexBackward(nextFirst);
//        checkResize();
        return item;
    }

    @Override
    public T removeLast(){
        if (size == 0) return null;

        T item = items[getLastIndex()];
        items[getLastIndex()] = null;
        size -= 1;
        nextLast = updateIndexForward(nextLast);
        checkResize();
        return item;
    }

    @Override
    public T get(int index){
        return items[(nextFirst + 1 + index) % items.length] ;
    }

    public Iterator<T> iterator(){
        return new ArrayDequeIterator();
    }

    public class ArrayDequeIterator implements Iterator<T>{
        private int pos;

        ArrayDequeIterator(){
            pos = 0;
        }

        @Override
        public boolean hasNext() {
            return size > pos;
        }

        @Override
        public T next(){
            T returnItem = (T) get(pos);
            pos += 1;
            return returnItem;
        }
    }

    public boolean eauals(Object o){
        if(o == this) return true;
        if(o == null) return false;
        if(!(o instanceof ArrayDeque)) return false;

        ArrayDeque<T> tmp = (ArrayDeque<T>) o;
        for(int i = 0;i < size;i++){
            if(!tmp.get(i).equals(this.get(i))) return false;
        }
        return true;
    }

    private void checkResize(){
        if(size < items.length / 4 + 1 && items.length > 17) {
            resize(items.length / 2);
        }
    }


}
