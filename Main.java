import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    // ---------------- Employee Model ----------------
    static class Employee {
        int id;
        String name;
        String department;
        double salary;
        LocalDate joiningDate;

        Employee(int id, String name, String department, double salary, LocalDate joiningDate) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.salary = salary;
            this.joiningDate = joiningDate;
        }

        int getId() { return id; }
        String getName() { return name; }
        String getDepartment() { return department; }
        double getSalary() { return salary; }
        LocalDate getJoiningDate() { return joiningDate; }
        void setSalary(double salary) { this.salary = salary; }

        @Override
        public String toString() {
            return String.format("ID:%d | %-10s | %-12s | $%-10.2f | Joined: %s",
                    id, name, department, salary, joiningDate);
        }
    }

    // ---------------- Functional Interface with default & static methods ----------------
    @FunctionalInterface
    interface SalaryOperation {
        double apply(double salary);

        default SalaryOperation andThen(SalaryOperation next) {
            return salary -> next.apply(this.apply(salary));
        }

        static SalaryOperation giveRaise(double percent) {
            return salary -> salary + (salary * percent / 100);
        }

        static SalaryOperation flatBonus(double amount) {
            return salary -> salary + amount;
        }
    }

    // ---------------- Employee Repository ----------------
    static class EmployeeRepository {
        private final List<Employee> employees = new ArrayList<>();

        void add(Employee e) { employees.add(e); }

        boolean remove(int id) {
            return employees.removeIf(e -> e.getId() == id);
        }

        Optional<Employee> findById(int id) {
            return employees.stream()
                    .filter(e -> e.getId() == id)
                    .findFirst();
        }

        void updateSalary(int id, SalaryOperation operation) {
            findById(id).ifPresentOrElse(
                    e -> {
                        double oldSalary = e.getSalary();
                        e.setSalary(operation.apply(oldSalary));
                        System.out.printf("Updated %s: $%.2f -> $%.2f%n", e.getName(), oldSalary, e.getSalary());
                    },
                    () -> System.out.println("No employee found with ID " + id)
            );
        }

        List<Employee> getAll() { return employees; }

        List<Employee> getByDepartment(String department) {
            return employees.stream()
                    .filter(e -> e.getDepartment().equalsIgnoreCase(department))
                    .collect(Collectors.toList());
        }

        Map<String, List<Employee>> groupByDepartment() {
            return employees.stream()
                    .collect(Collectors.groupingBy(Employee::getDepartment));
        }

        Map<String, Double> averageSalaryByDepartment() {
            return employees.stream()
                    .collect(Collectors.groupingBy(Employee::getDepartment,
                            Collectors.averagingDouble(Employee::getSalary)));
        }

        Map<String, Long> countByDepartment() {
            return employees.stream()
                    .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));
        }

        Optional<Employee> getHighestPaid() {
            return employees.stream()
                    .max(Comparator.comparingDouble(Employee::getSalary));
        }

        List<Employee> sortedBySalaryDesc() {
            return employees.stream()
                    .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                    .collect(Collectors.toList());
        }

        void printYearsOfService() {
            LocalDate today = LocalDate.now();
            employees.forEach(e -> {
                Period period = Period.between(e.getJoiningDate(), today);
                System.out.printf("%-10s -> %d years, %d months of service%n",
                        e.getName(), period.getYears(), period.getMonths());
            });
        }

        String namesAsString() {
            return employees.stream()
                    .map(Employee::getName)
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        DoubleSummaryStatistics salaryStatistics() {
            return employees.stream()
                    .collect(Collectors.summarizingDouble(Employee::getSalary));
        }

        boolean isEmpty() { return employees.isEmpty(); }
    }

    // ---------------- Main Program with User Input ----------------
    public static void main(String[] args) {
        EmployeeRepository repo = new EmployeeRepository();
        Scanner sc = new Scanner(System.in);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        boolean running = true;

        System.out.println("===== Employee Management System =====");

        while (running) {
            printMenu();
            System.out.print("Enter your choice: ");
            String choiceInput = sc.nextLine().trim();

            int choice;
            try {
                choice = Integer.parseInt(choiceInput);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid input. Please enter a number.\n");
                continue;
            }

            switch (choice) {
                case 1 -> addEmployee(sc, repo, dateFormatter);
                case 2 -> {
                    System.out.println("\n=== All Employees ===");
                    if (repo.isEmpty()) System.out.println("No employees added yet.");
                    else repo.getAll().forEach(System.out::println);
                }
                case 3 -> {
                    System.out.print("Enter department name: ");
                    String dept = sc.nextLine().trim();
                    List<Employee> list = repo.getByDepartment(dept);
                    if (list.isEmpty()) System.out.println("No employees found in " + dept);
                    else list.forEach(System.out::println);
                }
                case 4 -> updateSalary(sc, repo);
                case 5 -> {
                    System.out.print("Enter Employee ID to remove: ");
                    int id = readInt(sc);
                    System.out.println(repo.remove(id) ? "Employee removed." : "Employee not found.");
                }
                case 6 -> {
                    System.out.println("\n=== Grouped by Department ===");
                    repo.groupByDepartment().forEach((dept, list) -> {
                        System.out.println(dept + ":");
                        list.forEach(e -> System.out.println("   " + e));
                    });
                }
                case 7 -> {
                    System.out.println("\n=== Average Salary by Department ===");
                    repo.averageSalaryByDepartment().forEach((dept, avg) ->
                            System.out.printf("%-12s -> $%.2f%n", dept, avg));
                }
                case 8 -> {
                    System.out.println("\n=== Employee Count by Department ===");
                    repo.countByDepartment().forEach((dept, count) ->
                            System.out.println(dept + ": " + count));
                }
                case 9 -> {
                    System.out.println("\n=== Highest Paid Employee ===");
                    repo.getHighestPaid().ifPresentOrElse(
                            System.out::println,
                            () -> System.out.println("No employees found")
                    );
                }
                case 10 -> {
                    System.out.println("\n=== Employees Sorted by Salary (Descending) ===");
                    repo.sortedBySalaryDesc().forEach(System.out::println);
                }
                case 11 -> {
                    System.out.println("\n=== Years of Service ===");
                    if (repo.isEmpty()) System.out.println("No employees added yet.");
                    else repo.printYearsOfService();
                }
                case 12 -> {
                    System.out.println("\n=== Salary Statistics ===");
                    if (repo.isEmpty()) {
                        System.out.println("No employees added yet.");
                    } else {
                        DoubleSummaryStatistics stats = repo.salaryStatistics();
                        System.out.printf("Min: $%.2f | Max: $%.2f | Avg: $%.2f | Total: $%.2f | Count: %d%n",
                                stats.getMin(), stats.getMax(), stats.getAverage(), stats.getSum(), stats.getCount());
                    }
                }
                case 13 -> {
                    System.out.println("\n=== All Employee Names ===");
                    System.out.println(repo.namesAsString());
                }
                case 0 -> {
                    running = false;
                    System.out.println("Exiting. Report generated on: " + LocalDate.now().format(dateFormatter));
                }
                default -> System.out.println("Invalid choice. Try again.\n");
            }
            System.out.println();
        }

        sc.close();
    }

    private static void printMenu() {
        System.out.println("""
                --------------------------------------
                1.  Add Employee
                2.  View All Employees
                3.  View Employees by Department
                4.  Update Employee Salary (Raise / Bonus / Custom)
                5.  Remove Employee
                6.  Group Employees by Department
                7.  Average Salary by Department
                8.  Employee Count by Department
                9.  Highest Paid Employee
                10. Employees Sorted by Salary (Desc)
                11. Years of Service for Each Employee
                12. Salary Statistics
                13. All Employee Names
                0.  Exit
                --------------------------------------""");
    }

    private static void addEmployee(Scanner sc, EmployeeRepository repo, DateTimeFormatter formatter) {
        try {
            System.out.print("Enter Employee ID: ");
            int id = readInt(sc);

            System.out.print("Enter Name: ");
            String name = sc.nextLine().trim();

            System.out.print("Enter Department: ");
            String dept = sc.nextLine().trim();

            System.out.print("Enter Salary: ");
            double salary = Double.parseDouble(sc.nextLine().trim());

            System.out.print("Enter Joining Date (dd-MM-yyyy): ");
            String dateStr = sc.nextLine().trim();
            LocalDate joiningDate = LocalDate.parse(dateStr, formatter);

            repo.add(new Employee(id, name, dept, salary, joiningDate));
            System.out.println("Employee added successfully.");
        } catch (Exception e) {
            System.out.println("Invalid input. Employee not added. (" + e.getMessage() + ")");
        }
    }

    private static void updateSalary(Scanner sc, EmployeeRepository repo) {
        System.out.print("Enter Employee ID: ");
        int id = readInt(sc);

        System.out.println("Choose update type: 1) Percentage Raise  2) Flat Bonus  3) Raise + Bonus");
        System.out.print("Enter choice: ");
        int type = readInt(sc);

        switch (type) {
            case 1 -> {
                System.out.print("Enter raise percentage: ");
                double percent = Double.parseDouble(sc.nextLine().trim());
                repo.updateSalary(id, SalaryOperation.giveRaise(percent)); // static method
            }
            case 2 -> {
                System.out.print("Enter bonus amount: ");
                double amount = Double.parseDouble(sc.nextLine().trim());
                repo.updateSalary(id, SalaryOperation.flatBonus(amount)); // static method
            }
            case 3 -> {
                System.out.print("Enter raise percentage: ");
                double percent = Double.parseDouble(sc.nextLine().trim());
                System.out.print("Enter bonus amount: ");
                double amount = Double.parseDouble(sc.nextLine().trim());
                SalaryOperation combined = SalaryOperation.giveRaise(percent)
                        .andThen(SalaryOperation.flatBonus(amount)); // default method
                repo.updateSalary(id, combined);
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static int readInt(Scanner sc) {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid number, try again: ");
            }
        }
    }
}