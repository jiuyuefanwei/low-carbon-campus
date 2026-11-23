package campus.item;

import campus.jdbc.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteCategoryDAO implements CategoryDAO {

    private final Connection connection;

    public SqliteCategoryDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Category category) throws SQLException {
        String sql = "INSERT INTO category (name, description, carbon_factor, is_active) "
                + "VALUES (?, ?, COALESCE(?, 0), COALESCE(?, 1))";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Jdbc.bind(
                    statement,
                    category.getName(),
                    category.getDescription(),
                    category.getCarbonFactor(),
                    activeFlag(category.getIsActive())
            );
            statement.executeUpdate();
            category.setId(Jdbc.generatedId(statement));
        }
        Category stored = findById(category.getId());
        category.setCarbonFactor(stored.getCarbonFactor());
        category.setIsActive(stored.getIsActive());
        category.setCreatedAt(stored.getCreatedAt());
    }

    @Override
    public Category findById(Long id) throws SQLException {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM category WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(statement, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    @Override
    public List<Category> findActive() throws SQLException {
        String sql = "SELECT * FROM category WHERE is_active = 1 ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (resultSet.next()) {
                categories.add(map(resultSet));
            }
            return categories;
        }
    }

    @Override
    public int update(Category category) throws SQLException {
        String sql = "UPDATE category SET name = ?, description = ?, carbon_factor = ?, is_active = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Jdbc.bind(
                    statement,
                    category.getName(),
                    category.getDescription(),
                    category.getCarbonFactor(),
                    activeFlag(category.getIsActive()),
                    category.getId()
            );
            return statement.executeUpdate();
        }
    }

    private Integer activeFlag(Boolean isActive) {
        if (isActive == null) {
            return null;
        }
        return isActive ? 1 : 0;
    }

    private Category map(ResultSet resultSet) throws SQLException {
        Category category = new Category();
        category.setId(resultSet.getLong("id"));
        category.setName(resultSet.getString("name"));
        category.setDescription(resultSet.getString("description"));
        category.setCarbonFactor(resultSet.getDouble("carbon_factor"));
        category.setIsActive(resultSet.getInt("is_active") == 1);
        category.setCreatedAt(resultSet.getString("created_at"));
        return category;
    }
}
