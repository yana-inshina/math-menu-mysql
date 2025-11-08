# Консольное меню: byte/int/double + MySQL + Excel

## Требования
- JDK 17+
- MySQL 8.x (localhost:3306)

## Установка БД
1) В MySQL выполнить: `SOURCE create_db.sql;`
2) При необходимости сменить креды в `src/main/java/org/example/Main.java` (DB_URL/DB_USER/DB_PASS).

## Запуск
- Сборка JAR: `mvn -q -DskipTests package`
- Запуск: `java -jar target/math-menu-mysql-1.0.0-shaded.jar`

## Проверка пунктов меню
2 — создать/проверить `math_results`  
3–9 — операции по типам, результат → БД и в консоль  
10 — экспорт `math_results.xlsx` и вывод содержимого на экран

## Примечания
- Для `byte`/`int` деление целочисленное; для `double` — вещественное.
- Файл Excel создаётся рядом с исполняемым JAR (или в корне проекта при запуске из IDE).
