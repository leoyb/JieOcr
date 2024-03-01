package ocr.sdk.common;

import java.util.*;

public class FixedSizeQueue {
    private LinkedList<String> deque;

    private Map<Integer, FixedSizeQueue> awardHistory = new HashMap<>();
    private int maxSize;

    public FixedSizeQueue(int maxSize) {
        this.maxSize = maxSize;
        this.deque = new LinkedList<>();
        for(int i = 0; i < maxSize; i++) {
            this.deque.add("");
        }
    }

    public LinkedList<String> getDeque(int pos) {
        FixedSizeQueue deque = awardHistory.get(pos);
        if(null == deque) return new LinkedList<>();
        return awardHistory.get(pos).getDeque();
    }

    private LinkedList<String> getDeque() {
        return deque;
    }

    public void add(String element) {
        if (deque.size() == maxSize) {
            deque.poll(); // Remove the oldest element
        }
        deque.offer(element); // Add the new element
    }

    public String poll() {
        return deque.poll();
    }

    public int size() {
        return deque.size();
    }

    public void correctQueue() {
        if (deque.isEmpty()) return;

        int count = 1; // 记录连续字符出现的次数
        String prev = deque.poll(); // 取出队列的第一个字符
        String current;

        LinkedList<String> tempQueue = new LinkedList<>(); // 临时队列用于存储调整后的字符
        tempQueue.add(prev); // 将第一个字符加入临时队列

        while (!deque.isEmpty()) {
            current = deque.poll(); // 取出下一个字符
            if (Objects.equals(current, prev)) {
                // 如果当前字符与前一个字符相同，增加计数
                count++;
            } else {
                // 如果当前字符与前一个字符不同
                if (!deque.isEmpty() && Objects.equals(deque.peek(), prev)) {
                    // 如果下一个字符与前一个字符相同，则当前字符是偶然错误，需要调整
                    current = prev; // 将当前字符调整为前一个字符
                    count++; // 增加计数
                } else {
                    // 如果下一个字符与前一个字符不同，则重置计数
                    count = 1;
                }
            }
            tempQueue.add(current); // 将当前字符加入临时队列
            prev = current; // 更新前一个字符
        }

        // 将调整后的队列复制回原队列
        deque.clear();
        deque.addAll(tempQueue);
    }

    public String isMatch(String input, int cap) {
        String patternString = "";

        if (deque.size() < maxSize) {
            return patternString; // 队列未满，不检查条件
        }

        boolean valid = true; // 假设条件初始为满足
        for (int i = 0; i < deque.size(); i++) {
            if (i < cap) {
                // 检查前cap个元素不等于input
                if (deque.get(i).equals(input)) {
                    valid = false; // 如果前cap个中有input，则条件不满足
                    break; // 找到不满足条件的元素，跳出循环
                }
            } else {
                // 检查剩余的元素是否都等于input
                if (!deque.get(i).equals(input)) {
                    valid = false; // 如果剩余元素中有不等于input的，则条件不满足
                    break; // 找到不满足条件的元素，跳出循环
                }
            }
        }

        // 根据条件是否满足返回相应的字符串
        return valid ? input : patternString;
    }

    public String doMatch(int pos, String str, boolean isAward) {
        FixedSizeQueue history = awardHistory.getOrDefault(pos, new FixedSizeQueue(maxSize));
        history.add(str);
        if(!isAward) {
            history.correctQueue();
        }
        String result = history.isMatch(str, (int)Math.ceil(maxSize / 2.0));
        awardHistory.put(pos, history);
        return result;
    }


    public static void main(String[] args) {
        FixedSizeQueue queue = new FixedSizeQueue(4);
        queue.add("0");
        String out = queue.isMatch("0", 3);
        System.out.println("find:" + out );
        queue.add("0");
        out = queue.isMatch("1", 3);
        System.out.println("find:" + out );
//        queue.add("0");
        out = queue.isMatch("1", 3);
        System.out.println("find:" + out );
//        queue.add("1");
        out = queue.isMatch("1", 3);
        System.out.println("find:" + out );
//        queue.add("1");
        out = queue.isMatch("1", 3);
        System.out.println("find:" + out );
        queue.add("0");
        queue.correctQueue();
        System.out.println("find:" + queue );
        queue.add("0");
        queue.correctQueue();
        System.out.println("find:" + queue );
        queue.add("0");
        out = queue.isMatch("1", 3);
        queue.add("1");
        queue.correctQueue();
        System.out.println("find:" + queue );
        System.out.println("find:" + out );
        out = queue.isMatch("0", 3);
        queue.add("0");
        queue.correctQueue();
        System.out.println("find:" + queue );
        System.out.println("find:" + out );
        out = queue.isMatch("1", 2);
        queue.add("1");
        queue.correctQueue();
        queue.add("1");
        queue.correctQueue();
        System.out.println("find:" + out );


    }
}