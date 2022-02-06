package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.Entity;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CustomerRepository.class);

    private DataSource dataSource;

    public CustomerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Person createPersonFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String firstName = rs.getString(2);
        String lastName = rs.getString(3);
        String personalNumber = rs.getString(4);
        String address = rs.getString(5);
        return new Person(id, firstName, lastName, personalNumber, address);
    }

    public List<Customer> getCustomers() {
        List<com.zuehlke.securesoftwaredevelopment.domain.Customer> customers = new ArrayList<com.zuehlke.securesoftwaredevelopment.domain.Customer>();
        String query = "SELECT id, username FROM users";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                customers.add(createCustomer(rs));
            }

        } catch (SQLException e) {
            LOG.warn("Failed to get all users due to SQL Exception", e);
        }
        return customers;
    }

    private com.zuehlke.securesoftwaredevelopment.domain.Customer createCustomer(ResultSet rs) throws SQLException {
        return new com.zuehlke.securesoftwaredevelopment.domain.Customer(rs.getInt(1), rs.getString(2));
    }

    public List<Restaurant> getRestaurants() {
        List<Restaurant> restaurants = new ArrayList<Restaurant>();
        String query = "SELECT r.id, r.name, r.address, rt.name  FROM restaurant AS r JOIN restaurant_type AS rt ON r.typeId = rt.id ";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                restaurants.add(createRestaurant(rs));
            }

        } catch (SQLException e) {
            LOG.warn("Failed to get all restaurants due to SQL Exception", e);
        }
        return restaurants;
    }

    private Restaurant createRestaurant(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        String address = rs.getString(3);
        String type = rs.getString(4);

        return new Restaurant(id, name, address, type);
    }


    public Object getRestaurant(String id) {
        String query = "SELECT r.id, r.name, r.address, rt.name  FROM restaurant AS r JOIN restaurant_type AS rt ON r.typeId = rt.id WHERE r.id=" + id;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            if (rs.next()) {
                return createRestaurant(rs);
            }

        } catch (SQLException e) {
            LOG.warn("Failed to get restaurant with id = " + id + " due to SQL Exception", e);
        }
        return null;
    }

    public void deleteRestaurant(int id) {
        String query = "DELETE FROM restaurant WHERE id=" + id;
        Restaurant restaurant = (Restaurant) getRestaurant(String.valueOf(id));
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            auditLogger.audit("Deleted restaurant with id = " + id + "and name = " + restaurant.getName());
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOG.warn("Failed to delete restaurant with id = " + id + " due to SQL Exception", e);
        }
    }

    public void updateRestaurant(RestaurantUpdate restaurantUpdate) {
        Restaurant restaurant = (Restaurant) getRestaurant(String.valueOf(restaurantUpdate.getId()));
        String query = "UPDATE restaurant SET name = '" + restaurantUpdate.getName() + "', address='" + restaurantUpdate.getAddress() + "', typeId =" + restaurantUpdate.getRestaurantType() + " WHERE id =" + restaurantUpdate.getId();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            String before = String.format("name: %s, address: %s, type: %s",
                    restaurant.getName(), restaurant.getAddress(), restaurant.getRestaurantType());
            String after = String.format("name: %s, address: %s, type: %d",
                    restaurantUpdate.getName(), restaurantUpdate.getAddress(), restaurantUpdate.getRestaurantType());
            auditLogger.auditChange(new Entity("restaurant.update", String.valueOf(restaurant.getId()), before, after));
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOG.warn("Failed to update restaurant with id = "
                    + restaurantUpdate.getId() +", name = " + restaurantUpdate.getName() + " due to SQL Exception", e);
        }

    }

    public Customer getCustomer(String id) {
        String sqlQuery = "SELECT id, username, password FROM users WHERE id=" + id;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sqlQuery)) {

            if (rs.next()) {
                return createCustomerWithPassword(rs);
            }

        } catch (SQLException e) {
            LOG.warn("Failed to get user with id = " + id + " due to SQL Exception", e);
        }
        return null;
    }

    private Customer createCustomerWithPassword(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String username = rs.getString(2);
        String password = rs.getString(3);
        return new Customer(id, username, password);
    }


    public void deleteCustomer(String id) {
        Customer customer = getCustomer(String.valueOf(id));
        String query = "DELETE FROM users WHERE id=" + id;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            auditLogger.audit("Deleted customer with id = " + id + "and username = " + customer.getUsername());
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOG.warn("Failed to get delete user with id = " + id + " due to SQL Exception", e);
        }
    }

    public void updateCustomer(CustomerUpdate customerUpdate) {
        Customer customer = getCustomer(String.valueOf(customerUpdate.getId()));
        String query = "UPDATE users SET username = '" + customerUpdate.getUsername() + "', password='" + customerUpdate.getPassword() + "' WHERE id =" + customerUpdate.getId();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            String before = String.format("username: %s, password: %s", customer.getUsername(), customer.getPassword());
            String after = String.format("username: %s, password: %s", customerUpdate.getUsername(), customerUpdate.getPassword());
            auditLogger.auditChange(new Entity("customer.update", String.valueOf(customer.getId()), before, after));
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOG.warn("Failed to get update user with id = " + customerUpdate.getId() + " due to SQL Exception", e);
        }
    }

    public List<Address> getAddresses(String id) {
        String sqlQuery = "SELECT id, name FROM address WHERE userId=" + id;
        List<Address> addresses = new ArrayList<Address>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sqlQuery)) {

            while (rs.next()) {
                addresses.add(createAddress(rs));
            }

        } catch (SQLException e) {
            LOG.warn("Failed to get addresses for user with id = " + id + " due to SQL Exception", e);
        }
        return addresses;
    }

    private Address createAddress(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        return new Address(id, name);
    }

    public void deleteCustomerAddress(int id) {
        String query = "DELETE FROM address WHERE id=" + id;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            auditLogger.audit("Deleted customer address with id = " + id);
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOG.warn("Failed to delete address with id = " + id + " due to SQL Exception", e);
        }
    }

    public void updateCustomerAddress(Address address) {
        String query = "UPDATE address SET name = '" + address.getName() + "' WHERE id =" + address.getId();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            auditLogger.audit("Updated customer address with new name = " + address.getName());
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOG.warn("Failed to update address with id = " + address.getId() + " due to SQL Exception", e);
        }
    }

    public void putCustomerAddress(NewAddress newAddress) {
        String query = "INSERT INTO address (name, userId) VALUES ('"+newAddress.getName()+"' , "+newAddress.getUserId()+")";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            auditLogger.audit(String.format("Created new customer address with name = %s, userId = %d", newAddress.getName(), newAddress.getUserId()));
            statement.executeUpdate(query);
        } catch (SQLException e) {
            LOG.warn("Failed to create address with name = "
                    + newAddress.getName() + "for user with id = " + newAddress.getUserId() + " due to SQL Exception", e);
        }
    }
}
