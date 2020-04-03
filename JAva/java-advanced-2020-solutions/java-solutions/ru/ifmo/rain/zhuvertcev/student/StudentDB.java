package ru.ifmo.rain.zhuvertcev.student;
import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {

    private List<String> collectBy(List<Student> students, Function<Student, String> mapBy) {
        return students.stream().map(mapBy).collect(Collectors.toList());
    }

    private List<Student> sortStudentsBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return collectBy(students, Student::getFirstName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return collectBy(students, Student::getGroup);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return collectBy(students, Student::getLastName);
    }

    private String getFullName(Student student) {
        return student.getFirstName().concat(" ".concat(student.getLastName()));
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return collectBy(students, this::getFullName);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsBy(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsBy(students, Comparator.comparing(Student::getLastName).
                thenComparing(Student::getFirstName).thenComparing(Student::getId));
    }

    private List<Student> findStudentsBy(Collection<Student> students, Predicate<Student> predicate) {
        return sortStudentsByName(students.stream().filter(predicate).collect(Collectors.toList()));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, student -> student.getLastName().equals(name));
    }


    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, student -> student.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsBy(students, student -> student.getGroup().equals(group));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new TreeSet<>(collectBy(students, Student::getFirstName));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return findStudentsByGroup(students, group).stream().
                collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }

    private Stream<Map.Entry<String, List<Student>>> getStreamBy(Collection<Student> students,
                                                                 Function<Student, String> function) {
        return students.stream().collect(Collectors.groupingBy(function,
                TreeMap::new,
                Collectors.toList())).entrySet().stream();
    }

    private List<Group> getGroupListBy(Collection<Student> students, Function<List<Student>, List<Student>> sortBy) {
        return getStreamBy(students, Student::getGroup).map(group -> new Group(group.getKey(), sortBy.apply(group.getValue()))).collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupListBy(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupListBy(students, this::sortStudentsById);
    }

    public String getLargestGroupOf(Collection<Student> students, ToIntFunction<List<Student>> integerFunction) {
        return getStreamBy(students, Student::getGroup).max(Comparator.comparingInt(
                (Map.Entry<String, List<Student>> group) -> integerFunction.applyAsInt(group.getValue()))
                .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey).orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupOf(students, List::size);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupOf(students, list -> getDistinctFirstNames(list).size());
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(this::getFullName,
                TreeMap::new,
                Collectors.groupingBy(Student::getGroup))).entrySet().stream().
                max(Comparator.comparingInt(( Map.Entry<String, Map<String, List<Student>>> studentName) ->
                        studentName.getValue().size()).
                        thenComparing(Map.Entry::getKey, String::compareTo)).map(Map.Entry::getKey).orElse("");
    }

    private List<Student> getStudentsByIndices(List<Student> students, int[] indices) {
        return IntStream.of(indices).boxed().mapToInt(x -> x).mapToObj(students::get).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getFirstNames(getStudentsByIndices(new ArrayList<>(students), indices));
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getLastNames(getStudentsByIndices(new ArrayList<>(students), indices));
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getGroups(getStudentsByIndices(new ArrayList<>(students), indices));
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getFullNames(getStudentsByIndices(new ArrayList<>(students), indices));
    }
}
