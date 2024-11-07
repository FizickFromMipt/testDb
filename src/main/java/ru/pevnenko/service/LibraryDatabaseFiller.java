package ru.pevnenko.service;

import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Component
public class LibraryDatabaseFiller implements CommandLineRunner {
    private final List<String> russianGenres = Arrays.asList("Роман", "Фантастика", "Поэзия", "Детектив", "Научная литература", "Приключения", "Фэнтези", "Исторический роман");

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${library.authors.count}")
    private int authorsCount;

    @Value("${library.books.count}")
    private int booksCount;

    @Value("${library.borrowers.count}")
    private int borrowersCount;

    private final DataSource dataSource;
    private final Faker ruFaker = new Faker(new Locale("ru"));
    private final Faker enFaker = new Faker();
    private final Random random = new Random();

    public LibraryDatabaseFiller(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            clearDatabase(connection);

            int addedAuthors = 0;
            for (int i = 0; i < authorsCount; i++) {
                String name = ruFaker.book().author();
                addAuthor(connection, name);
                addedAuthors++;
            }

            for (int i = 0; i < booksCount; i++) {
                // Генерируем название книги на основе случайных русских слов
                String title = ruFaker.book().title();
                // Выбираем случайный жанр из списка
                String genre = russianGenres.get(random.nextInt(russianGenres.size()));
                int publishedYear = 1900 + random.nextInt(122);
                Integer authorId = 1 + random.nextInt(addedAuthors);
                addBook(connection, title, genre, publishedYear, authorId);
            }

            for (int i = 0; i < borrowersCount; i++) {
                String name = ruFaker.name().fullName();
                String email = enFaker.internet().emailAddress();
                Integer borrowedBookId = random.nextBoolean() ? 1 + random.nextInt(booksCount) : null;
                addBorrower(connection, name, email, borrowedBookId);
            }

            connection.commit();
            System.out.println("Database filled successfully with random data in Russian.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearDatabase(Connection connection) throws SQLException {
        String[] tables = {"borrowers", "books", "authors"};
        for (String table : tables) {
            try (PreparedStatement statement = connection.prepareStatement("TRUNCATE TABLE " + table + " RESTART IDENTITY CASCADE")) {
                statement.executeUpdate();
            }
        }
        System.out.println("Old data deleted and IDs reset successfully.");
    }

    private void addAuthor(Connection connection, String name) throws SQLException {
        String sql = "INSERT INTO authors (name) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private void addBook(Connection connection, String title, String genre, int year, Integer authorId) throws SQLException {
        String sql = "INSERT INTO books (title, genre, published_year, author_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, genre);
            statement.setInt(3, year);
            if (authorId != null) {
                statement.setInt(4, authorId);
            } else {
                statement.setNull(4, Types.INTEGER);
            }
            statement.executeUpdate();
        }
    }

    private void addBorrower(Connection connection, String name, String email, Integer borrowedBookId) throws SQLException {
        String sql = "INSERT INTO borrowers (name, email, borrowed_book_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            if (borrowedBookId != null) {
                statement.setInt(3, borrowedBookId);
            } else {
                statement.setNull(3, Types.INTEGER);
            }
            statement.executeUpdate();
        }
    }
}
