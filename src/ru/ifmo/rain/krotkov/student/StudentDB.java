package ru.ifmo.rain.krotkov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {
    private static final Comparator<Student> STUDENT_BY_NAME_COMPARATOR = Comparator
            .comparing(Student::getLastName, String::compareTo)
            .thenComparing(Student::getFirstName, String::compareTo)
            .thenComparingInt(Student::getId);

    private <T, C extends Collection<T>> C mapToCollection(Collection<Student> students, Function<Student, T> mapper,
                                                           Supplier<C> collector) {
        return students.stream()
                .map(mapper)
                .collect(Collectors.toCollection(collector));
    }

    private <T> List<T> mapToList(Collection<Student> students, Function<Student, T> mapper) {
        return mapToCollection(students, mapper, ArrayList::new);
    }

    private List<Student> sortToList(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator)
                .collect(Collectors.toList());
    }

    private Stream<Student> filterToStream(Stream<Student> students, Predicate<Student> condition) {
        return students.filter(condition);
    }

    private List<Student> filterAndSortToList(Collection<Student> students, Predicate<Student> condition) {
        return filterToStream(students.stream(), condition).sorted(STUDENT_BY_NAME_COMPARATOR)
                .collect(Collectors.toList());
    }

    private Stream<Map.Entry<String, List<Student>>> groupToEntriesStream(Stream<Student> students) {
        return students
                .collect(Collectors.groupingBy(Student::getGroup, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream();
    }

    private Stream<Map.Entry<String, List<Student>>> groupToSortedEntriesStream(Stream<Student> students,
                                                                                Comparator<Student> comparator) {
        return groupToEntriesStream(students.sorted(comparator));
    }

    private List<Group> sortToGroupList(Collection<Student> students, Comparator<Student> comparator) {
        return groupToSortedEntriesStream(students.stream(), comparator)
                .map(entry -> new Group(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Group::getName, String::compareTo))
                .collect(Collectors.toList());
    }

    private String getLargestGroupBy(Stream<Map.Entry<String, List<Student>>> groups,
                                     Comparator<List<Student>> comparator) {
        return groups
                .max(Map.Entry.<String, List<Student>>comparingByValue(comparator)
                        .thenComparing(Map.Entry.comparingByKey(Collections.reverseOrder(String::compareTo))))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private Predicate<Student> groupPredicate(String group) {
        return student -> Objects.equals(student.getGroup(), group);
    }

    private String fullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapToList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapToList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapToList(students, this::fullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapToCollection(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortToList(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortToList(students, STUDENT_BY_NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterAndSortToList(students, student -> Objects.equals(student.getFirstName(), name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterAndSortToList(students, student -> Objects.equals(student.getLastName(), name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filterAndSortToList(students, groupPredicate(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return filterToStream(students.stream(), groupPredicate(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return sortToGroupList(students, STUDENT_BY_NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return sortToGroupList(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(groupToEntriesStream(students.stream()),
                Comparator.comparingInt(List::size));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(groupToEntriesStream(students.stream()),
                Comparator.comparingInt(list -> getDistinctFirstNames(list).size()));
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(
                        this::fullName,
                        Collectors.mapping(Student::getGroup, Collectors.toSet()))
                )
                .entrySet().stream()
                .max(Map.Entry.<String, Set<String>>comparingByValue(Comparator.comparingInt(Set::size)).thenComparing(
                        Map.Entry.comparingByKey(String::compareTo)))
                .map(Map.Entry::getKey)
                .orElse("");
    }
}
