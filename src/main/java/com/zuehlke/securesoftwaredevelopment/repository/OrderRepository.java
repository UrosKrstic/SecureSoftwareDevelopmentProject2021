package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private DataSource dataSource;
    private static final Logger LOG = LoggerFactory.getLogger(OrderRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(OrderRepository.class);

    public OrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public List<Food> getMenu(int id) {
        List<Food> menu = new ArrayList<>();
        String sqlQuery = "SELECT id, name FROM food WHERE restaurantId= ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery)
        ) {
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                menu.add(createFood(rs));
            }

        } catch (SQLException e) {
             LOG.warn("Failed to get menu with id = " + id + ", due to SQL Exception", e);
        }

        return menu;
    }

    private Food createFood(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        return new Food(id, name);
    }

    public void insertNewOrder(NewOrder newOrder, int userId) {
        LocalDate date = LocalDate.now();
        String sqlQuery = "INSERT INTO delivery (isDone, userId, restaurantId, addressId, date, comment)" +
                "values (FALSE, ?, ?, ?, ?, ?)";
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sqlQuery);
            statement.setInt(1, userId);
            statement.setInt(2, newOrder.getRestaurantId());
            statement.setInt(3, newOrder.getAddress());
            statement.setDate(4, java.sql.Date.valueOf(date));
            statement.setString(5, newOrder.getComment());
            statement.executeUpdate();

            sqlQuery = "SELECT MAX(id) FROM delivery";
            statement = connection.prepareStatement(sqlQuery);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {

                int deliveryId = rs.getInt(1);

                auditLogger.audit(String.format("Created new order: deliveryId = %d, userId = %d, restaurantId = %d, addressId = %d, date = %s",
                        deliveryId, userId,newOrder.getRestaurantId(), newOrder.getAddress(), date.toString()));

                sqlQuery = "INSERT INTO delivery_item (amount, foodId, deliveryId)" +
                        "values";
                for (int i = 0; i < newOrder.getItems().length; i++) {
                    String deliveryItem = "";
                    if (i > 0) {
                        deliveryItem = ",";
                    }
                    deliveryItem += "(?,?,?)";
                    sqlQuery += deliveryItem;
                }
                statement = connection.prepareStatement(sqlQuery);
                for (int i = 0; i < newOrder.getItems().length; i++) {
                    FoodItem item = newOrder.getItems()[i];
                    statement.setInt(1 + i * 3, item.getAmount());
                    statement.setInt(2 + i * 3, item.getFoodId());
                    statement.setInt(3 + i * 3, deliveryId);
                    auditLogger.audit(String.format("With item: amount = %d, foodId = %d, deliveryId = %d",
                            item.getAmount(), item.getFoodId(), deliveryId));
                }
                statement.executeUpdate();
            }

        } catch (SQLException e) {
            LOG.warn("Failed to create new order due to SQL Exception", e);
        }


    }

    public Object getAddresses(int userId) {
        List<Address> addresses = new ArrayList<>();
        String sqlQuery = "SELECT id, name FROM address WHERE userId= ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery)
        ) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                addresses.add(createAddress(rs));
            }

        } catch (SQLException e) {
            LOG.warn("Failed to get address with userId = " + userId + ", due to SQL Exception", e);
        }
        return addresses;
    }

    private Address createAddress(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        return new Address(id, name);

    }
}
