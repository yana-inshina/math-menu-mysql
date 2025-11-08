package org.example;

import java.sql.*;
import java.util.*;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Main {

    // ==== НАСТРОЙКИ БАЗЫ ДАННЫХ ====
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/mathdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "app";       // при необходимости поменяйте на своего пользователя
    private static final String DB_PASS = "password";  // и пароль

    enum DType { BYTE, INT, DOUBLE }
    enum Op { ADD, SUB, MUL, DIV, MOD, ABS, POW }

    public static void main(String[] args) {
        try (Connection cn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Scanner sc = new Scanner(System.in)) {

            ensureResultsTable(cn);

            while (true) {
                printMenu();
                System.out.print("Ваш выбор (0 — выход): ");
                String choice = sc.nextLine().trim();

                switch (choice) {
                    case "0":
                        System.out.println("Выход. До встречи!");
                        return;
                    case "1":
                        listAllTables(cn);
                        break;
                    case "2":
                        ensureResultsTable(cn);
                        System.out.println("Таблица math_results создана/проверена.");
                        break;
                    case "3":
                        handleBinaryMath(sc, cn, Op.ADD);
                        break;
                    case "4":
                        handleBinaryMath(sc, cn, Op.SUB);
                        break;
                    case "5":
                        handleBinaryMath(sc, cn, Op.MUL);
                        break;
                    case "6":
                        handleDivision(sc, cn);
                        break;
                    case "7":
                        handleModulo(sc, cn);
                        break;
                    case "8":
                        handleAbs(sc, cn);
                        break;
                    case "9":
                        handlePow(sc, cn);
                        break;
                    case "10":
                        exportToExcel(cn, "math_results.xlsx");
                        break;
                    default:
                        System.out.println("Неизвестная команда. Повторите ввод.");
                }
                System.out.println();
            }

        } catch (SQLException e) {
            System.err.println("Ошибка БД: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("====================================");
        System.out.println("1.  Вывести все таблицы из MySQL");
        System.out.println("2.  Создать таблицу в MySQL");
        System.out.println("3.  Сложение чисел (сохранить в MySQL, вывести в консоль)");
        System.out.println("4.  Вычитание чисел (сохранить в MySQL, вывести в консоль)");
        System.out.println("5.  Умножение чисел (сохранить в MySQL, вывести в консоль)");
        System.out.println("6.  Деление чисел (сохранить в MySQL, вывести в консоль)");
        System.out.println("7.  Деление по модулю (остаток) (сохранить в MySQL, вывести в консоль)");
        System.out.println("8.  Модуль числа (сохранить в MySQL, вывести в консоль)");
        System.out.println("9.  Возведение в степень (сохранить в MySQL, вывести в консоль)");
        System.out.println("10. Сохранить все данные из MySQL в Excel и вывести на экран");
        System.out.println("====================================");
    }

    private static Main.DType askType(Scanner sc) {
        while (true) {
            System.out.print("Выберите тип (1-byte, 2-int, 3-double): ");
            String s = sc.nextLine().trim();
            switch (s) {
                case "1": return DType.BYTE;
                case "2": return DType.INT;
                case "3": return DType.DOUBLE;
                default: System.out.println("Неверный выбор, попробуйте ещё.");
            }
        }
    }

    private static Number readOne(Scanner sc, DType t, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                switch (t) {
                    case BYTE:
                        return Byte.parseByte(sc.nextLine().trim());
                    case INT:
                        return Integer.parseInt(sc.nextLine().trim());
                    case DOUBLE:
                        return Double.parseDouble(sc.nextLine().trim());
                }
            } catch (NumberFormatException ex) {
                System.out.println("Ошибка ввода. Попробуйте ещё.");
            }
        }
    }

    private static void handleBinaryMath(Scanner sc, Connection cn, Op op) {
        DType t = askType(sc);
        Number a = readOne(sc, t, "Введите a: ");
        Number b = readOne(sc, t, "Введите b: ");

        String expr;
        double res;

        switch (op) {
            case ADD:
                expr = "a + b";
                res = add(t, a, b);
                break;
            case SUB:
                expr = "a - b";
                res = sub(t, a, b);
                break;
            case MUL:
                expr = "a * b";
                res = mul(t, a, b);
                break;
            default:
                throw new IllegalStateException("Unsupported op for handleBinaryMath: " + op);
        }

        insertResult(cn, t, op, a, b, res, null);
        System.out.printf("%s = %s%n", expr, formatResult(t, res));
    }

    private static void handleDivision(Scanner sc, Connection cn) {
        DType t = askType(sc);
        Number a = readOne(sc, t, "Введите a: ");
        Number b = readOne(sc, t, "Введите b: ");

        if (isZero(t, b)) {
            System.out.println("Деление на ноль невозможно.");
            insertResult(cn, t, Op.DIV, a, b, Double.NaN, "division by zero");
            return;
        }

        double res = div(t, a, b);
        insertResult(cn, t, Op.DIV, a, b, res, null);
        System.out.printf("a / b = %s%n", formatResult(t == DType.DOUBLE ? DType.DOUBLE : DType.INT, res));
    }

    private static void handleModulo(Scanner sc, Connection cn) {
        DType t = askType(sc);
        Number a = readOne(sc, t, "Введите a: ");
        Number b = readOne(sc, t, "Введите b: ");

        if (isZero(t, b)) {
            System.out.println("Остаток при делении на ноль невозможен.");
            insertResult(cn, t, Op.MOD, a, b, Double.NaN, "mod by zero");
            return;
        }

        double res = mod(t, a, b);
        insertResult(cn, t, Op.MOD, a, b, res, null);
        System.out.printf("a %% b = %s%n", formatResult(t == DType.DOUBLE ? DType.DOUBLE : DType.INT, res));
    }

    private static void handleAbs(Scanner sc, Connection cn) {
        DType t = askType(sc);
        Number a = readOne(sc, t, "Введите a: ");

        double res = abs(t, a);
        insertResult(cn, t, Op.ABS, a, null, res, null);
        System.out.printf("|a| = %s%n", formatResult(t, res));
    }

    private static void handlePow(Scanner sc, Connection cn) {
        DType t = askType(sc);
        Number a = readOne(sc, t, "Введите основание a: ");

        Number p;
        if (t == DType.DOUBLE) {
            p = readOne(sc, DType.DOUBLE, "Введите показатель степени (double): ");
        } else {
            p = readOne(sc, DType.INT, "Введите показатель степени (int): ");
        }

        double res = pow(t, a, p);
        insertResult(cn, t, Op.POW, a, p, res, null);
        System.out.printf("a^p = %s%n", formatResult(DType.DOUBLE, res));
    }

    private static boolean isZero(DType t, Number x) {
        switch (t) {
            case BYTE:  return x.byteValue() == 0;
            case INT:   return x.intValue() == 0;
            case DOUBLE:return Math.abs(x.doubleValue()) == 0.0;
        }
        return false;
    }

    private static double add(DType t, Number a, Number b) {
        switch (t) {
            case BYTE:  return (double)(a.byteValue() + b.byteValue());
            case INT:   return (double)(a.intValue() + b.intValue());
            case DOUBLE:return a.doubleValue() + b.doubleValue();
        }
        throw new IllegalStateException();
    }

    private static double sub(DType t, Number a, Number b) {
        switch (t) {
            case BYTE:  return (double)(a.byteValue() - b.byteValue());
            case INT:   return (double)(a.intValue() - b.intValue());
            case DOUBLE:return a.doubleValue() - b.doubleValue();
        }
        throw new IllegalStateException();
    }

    private static double mul(DType t, Number a, Number b) {
        switch (t) {
            case BYTE:  return (double)(a.byteValue() * b.byteValue());
            case INT:   return (double)(a.intValue() * b.intValue());
            case DOUBLE:return a.doubleValue() * b.doubleValue();
        }
        throw new IllegalStateException();
    }

    private static double div(DType t, Number a, Number b) {
        switch (t) {
            case BYTE:  return (double)(a.byteValue() / b.byteValue());
        case INT:   return (double)(a.intValue() / b.intValue());
            case DOUBLE:return a.doubleValue() / b.doubleValue();
        }
        throw new IllegalStateException();
    }

    private static double mod(DType t, Number a, Number b) {
        switch (t) {
            case BYTE:  return (double)(a.byteValue() % b.byteValue());
            case INT:   return (double)(a.intValue() % b.intValue());
            case DOUBLE:return a.doubleValue() % b.doubleValue();
        }
        throw new IllegalStateException();
    }

    private static double abs(DType t, Number a) {
        switch (t) {
            case BYTE:  return Math.abs(a.byteValue());
            case INT:   return Math.abs(a.intValue());
            case DOUBLE:return Math.abs(a.doubleValue());
        }
        throw new IllegalStateException();
    }

    private static double pow(DType t, Number a, Number p) {
        switch (t) {
            case BYTE:
            case INT:
                return Math.pow(a.doubleValue(), p.intValue());
            case DOUBLE:
                return Math.pow(a.doubleValue(), p.doubleValue());
        }
        throw new IllegalStateException();
    }

    private static String formatResult(DType t, double v) {
        if (Double.isNaN(v)) return "NaN";
        switch (t) {
            case BYTE:  return String.valueOf((int) v);
            case INT:   return String.valueOf((long) v);
            case DOUBLE:
                String s = String.valueOf(v);
                return s.contains("E") ? String.format(Locale.US, "%.10f", v) : s;
        }
        return String.valueOf(v);
    }

    private static void ensureResultsTable(Connection cn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS math_results (" +
                " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                " dtype VARCHAR(10) NOT NULL," +
                " operation VARCHAR(10) NOT NULL," +
                " a_val DOUBLE NULL," +
                " b_val DOUBLE NULL," +
                " result_val DOUBLE NOT NULL," +
                " a_text VARCHAR(64) NULL," +
                " b_text VARCHAR(64) NULL," +
                " result_text VARCHAR(128) NOT NULL," +
                " note VARCHAR(255) NULL" +
                ")";
        try (Statement st = cn.createStatement()) {
            st.execute(sql);
        }
    }

    private static void listAllTables(Connection cn) throws SQLException {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_name";
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("Таблицы в текущей базе:");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.println(" - " + rs.getString(1));
            }
            if (!any) System.out.println("(нет таблиц)");
        }
    }

    private static void insertResult(Connection cn, DType t, Op op, Number a, Number b, double res, String note) {
        String sql = "INSERT INTO math_results(dtype, operation, a_val, b_val, result_val, a_text, b_text, result_text, note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, t.name());
            ps.setString(2, op.name());
            if (a != null) {
                ps.setObject(3, a.doubleValue());
                ps.setString(6, a.toString());
            } else {
                ps.setNull(3, Types.DOUBLE);
                ps.setNull(6, Types.VARCHAR);
            }
            if (b != null) {
                ps.setObject(4, b.doubleValue());
                ps.setString(7, b.toString());
            } else {
                ps.setNull(4, Types.DOUBLE);
                ps.setNull(7, Types.VARCHAR);
            }
            ps.setDouble(5, res);
            String rText;
            if (Double.isNaN(res)) rText = "NaN";
            else if (t == DType.DOUBLE || op == Op.DIV || op == Op.POW) rText = String.valueOf(res);
            else rText = String.valueOf((long) res);
            ps.setString(8, rText);
            if (note != null) ps.setString(9, note); else ps.setNull(9, Types.VARCHAR);

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка вставки результата: " + e.getMessage());
        }
    }

    private static List<Map<String,Object>> selectAllResults(Connection cn) throws SQLException {
        String sql = "SELECT id, created_at, dtype, operation, a_text, b_text, result_text " +
                     "FROM math_results ORDER BY id";
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<Map<String,Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("created_at", rs.getTimestamp("created_at"));
                row.put("dtype", rs.getString("dtype"));
                row.put("operation", rs.getString("operation"));
                row.put("a", rs.getString("a_text"));
                row.put("b", rs.getString("b_text"));
                row.put("result", rs.getString("result_text"));
                rows.add(row);
            }
            return rows;
        }
    }

    private static void exportToExcel(Connection cn, String fileName) {
        try {
            List<Map<String,Object>> rows = selectAllResults(cn);

            if (rows.isEmpty()) {
                System.out.println("В таблице math_results пока нет данных.");
            } else {
                System.out.println("Данные для экспорта:");
                for (Map<String,Object> r : rows) {
                    System.out.printf("#%s | %s | %s | %s | a=%s | b=%s | result=%s%n",
                            r.get("id"), r.get("created_at"), r.get("dtype"), r.get("operation"),
                            Objects.toString(r.get("a"), "null"),
                            Objects.toString(r.get("b"), "null"),
                            r.get("result"));
                }
            }

            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("results");
                int rowIdx = 0;

                Row header = sheet.createRow(rowIdx++);
                String[] heads = {"id","created_at","dtype","operation","a","b","result"};
                for (int i = 0; i < heads.length; i++) header.createCell(i).setCellValue(heads[i]);

                for (Map<String,Object> r : rows) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(((Number)r.get("id")).longValue());
                    row.createCell(1).setCellValue(Objects.toString(r.get("created_at"), ""));
                    row.createCell(2).setCellValue(Objects.toString(r.get("dtype"), ""));
                    row.createCell(3).setCellValue(Objects.toString(r.get("operation"), ""));
                    row.createCell(4).setCellValue(Objects.toString(r.get("a"), ""));
                    row.createCell(5).setCellValue(Objects.toString(r.get("b"), ""));
                    row.createCell(6).setCellValue(Objects.toString(r.get("result"), ""));
                }

                for (int i = 0; i < heads.length; i++) sheet.autoSizeColumn(i);

                try (FileOutputStream out = new FileOutputStream(fileName)) {
                    wb.write(out);
                }
            }

            System.out.println("Экспорт завершён: " + fileName);

        } catch (Exception e) {
            System.err.println("Ошибка экспорта в Excel: " + e.getMessage());
        }
    }
}
