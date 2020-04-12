package ru.ifmo.rain.zhuvertcev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.min;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    /**
     * Create IterativeParallelism with default mapper
     */
    public IterativeParallelism() { parallelMapper = null;}

    /**
     * Create IterativeParallelism with given mapper
     *
     * @param parallelMapper mapper for parallel
     */
    public IterativeParallelism(ParallelMapper parallelMapper) { this.parallelMapper = parallelMapper;}

    private <T, E> E parallelReduce(int threadCount, final List<? extends T> values,
                                    Function<Stream<? extends T>, ? extends E> mapper,
                                    Function<Stream<? extends E>, ? extends E> joiner) throws InterruptedException {
        int necessaryThreadCount = min(values.size(), threadCount);
        int sliceSize = values.size() / necessaryThreadCount;
        int restSize = values.size() % necessaryThreadCount;
        List<Stream<? extends T>> slices = new ArrayList<>();
        for (int i = 0, to, from = 0; i < necessaryThreadCount; i++) {
            to = sliceSize + from;
            if (i < restSize) {
                to++;
            }
            slices.add(values.subList(from, to).stream());
            from = to;
        }
        List<Thread> threadsList = new ArrayList<>();
        List<E> threadsResults;
        if (parallelMapper != null) {
            threadsResults = parallelMapper.map(mapper, slices);
        } else {
            threadsResults = new ArrayList<>(Collections.nCopies(necessaryThreadCount, null));

            for (int i = 0; i < necessaryThreadCount; i++) {
                final int pos = i;
                threadsList.add(new Thread(() -> threadsResults.set(pos, mapper.apply(slices.get(pos)))));
                threadsList.get(i).start();
            }

            InterruptedException threadSuppressedExceptions = null;
            for (Thread thread : threadsList) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    if (threadSuppressedExceptions == null) {
                        threadSuppressedExceptions = e;
                    } else {
                        threadSuppressedExceptions.addSuppressed(e);
                    }
                }
            }

            if (threadSuppressedExceptions != null) {
                throw threadSuppressedExceptions;
            }
        }

        return joiner.apply(threadsResults.stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        Function<Stream<? extends T>, ? extends T> streamMax = stream -> stream.max(comparator).orElse(null);
        return parallelReduce(threads, values, streamMax, streamMax);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelReduce(threads, values, stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(bool -> bool == Boolean.TRUE));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelReduce(threads, values, stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(bool -> bool == Boolean.TRUE));
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelReduce(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining("")),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelReduce(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f)
            throws InterruptedException {
        return parallelReduce(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, element -> element, monoid);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid)
            throws InterruptedException {
        Function<Stream<? extends T>, ? extends R> applyMapMonoid = stream -> stream.map(element -> (T) element).map(lift)
                .reduce(monoid.getIdentity(), monoid.getOperator());
        Function<Stream<? extends R>, ? extends R> applyMonoid = stream -> stream.map(element -> (R) element)
                .reduce(monoid.getIdentity(), monoid.getOperator());
        return parallelReduce(threads, values, applyMapMonoid, applyMonoid);
    }
}
