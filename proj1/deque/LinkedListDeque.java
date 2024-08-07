package deque;

import java.util.Iterator;


public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {
    private class Node {
        private T value;
        private Node prev;
        private Node next;
        Node(T value, Node prev, Node next) {
            this.value = value;
            this.prev = prev;
            this.next = next;
        }
    }
    private Node nowNode; //也就是划分点
    private int size;

    public LinkedListDeque() {
        nowNode = new Node(null, null, null);
        nowNode.prev = nowNode;
        nowNode.next = nowNode;
        size = 0;
    }
    public void addFirst(T item) { // 相对于nowNode的后面，保持nowNode在前面，两种插入就围绕nowNode的两侧来进行，围城一圈
        Node nextFirstNode = nowNode.next;
        Node node = new Node(item, nowNode, nextFirstNode); // 在nowNode后边插入值
        nowNode.next = node;
        nextFirstNode.prev = node;
        size++;
    }
    public void addLast(T item) { // 相对于nowNode的前面加东西，保持nowNode在最后
        Node preLastNode = nowNode.prev;
        Node node = new Node(item, preLastNode, nowNode); // 由node向两边连接
        nowNode.prev = node; // 由两边向node连接，实现双端队列
        preLastNode.next = node;
        size++;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        Node tmpNode = nowNode.next;
        while (tmpNode != nowNode) {
            System.out.print(tmpNode.value + " ");
            tmpNode = tmpNode.next;
        }
        System.out.println();
    }


    @Override
    public T removeFirst() {
        Node firstNode = nowNode.next;
        if (firstNode == nowNode) {
            return null;
        }
        Node nextFirstNode = firstNode.next;
        T item = firstNode.value;
        nowNode.next = nextFirstNode;
        nextFirstNode.prev = nowNode;
        firstNode.prev = null;
        firstNode.next = null;
        size--;
        return item;
    }

    @Override
    public T removeLast() {
        Node lastNode = nowNode.prev;
        if (lastNode == nowNode) {
            return null;
        }
        Node preLastNode = lastNode.prev;
        T item = lastNode.value;
        nowNode.prev = preLastNode;
        preLastNode.next = nowNode;
        lastNode.prev = null;
        lastNode.next = null;
        size--;
        return item;
    }

    @Override
    public T get(int index) {
        Node cntNode = nowNode.next;
        for (int i = 0; i < size; i++) {
            if (index == i) {
                return cntNode.value;
            }
            cntNode = cntNode.next;
        }
        return null;
    }

    private T targetNodeValue(int index, Node targetNode) { //递归实现寻找index下标的值
        if (targetNode == nowNode) {
            return null;
        }
        if (index == 0) {
            return targetNode.value;
        }
        return targetNodeValue(index - 1, targetNode.next);

    }

    public T getRecursive(int index) {
        return targetNodeValue(index, nowNode.next);
    }

    public Iterator<T> iterator() {
        return new LinkedListIterator();
    }

    private class LinkedListIterator implements Iterator<T> {
        private Node iter;
        LinkedListIterator() {
            iter = nowNode.next;
        }

        @Override
        public boolean hasNext() {
            return iter.next != nowNode;
        }

        @Override
        public T next() {
            T returnItem = iter.value;
            iter = iter.next;
            return returnItem;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof Deque)) {
            return false;
        }
        Deque<T> tmp = (Deque<T>) o;
        if (size != tmp.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            T item = get(i);
            T item2 = tmp.get(i);
            if (!item.equals(item2)) {
                return false;
            }
        }
        return true;
    }

}
