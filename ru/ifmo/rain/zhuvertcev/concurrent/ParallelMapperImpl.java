package ru.ifmo.rain.zhuvertcev.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private static class ResultCollector<R> {
        private final List<R> result;
        private int addCounter;
        private final int needSize;

        public ResultCollector(int needSize) {
            result = new LinkedList<>(Collections.nCopies(needSize, null));
            addCounter = 0;
            this.needSize = needSize;
        }

        public void set(final int pos, R element) {
            result.set(pos, element);
            synchronized (this) {
                addCounter++;
                if (addCounter == needSize) {
                    notify();
                }
            }
        }

        synchronized public List<R> getResults() throws InterruptedException {
            while (addCounter != needSize) {
                wait();
            }
            return result;
        }
    }

    private final List<Thread> threads;
    private final Deque<Runnable> tasks;

    public ParallelMapperImpl(final int threadsNumber) {
        threads = new ArrayList<>();
        tasks = new ArrayDeque<>();
        for (int i = 0; i < threadsNumber; i++) {
            threads.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        doTask();
                    }
                } catch (InterruptedException ignored) {}
                finally {
                    Thread.currentThread().interrupt();
                }
            }));
            threads.get(i).start();
        }
    }

    private void doTask() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notifyAll();;
        }
        task.run();
    }

    private void addTask(Runnable task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notifyAll();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        ResultCollector<R> resultCollector = new ResultCollector<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int pos = i;
            addTask(() -> resultCollector.set(pos, f.apply(args.get(pos))));
        }
        return resultCollector.getResults();
    }

    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignore) {
            }
        }
    }
}
