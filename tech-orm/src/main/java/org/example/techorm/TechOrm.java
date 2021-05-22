package org.example.techorm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class TechOrm {

    private final Connection connection;
    private final boolean showSql;

    public TechOrm(Database database, boolean showSql, String host, int port, String db, String user, String password) throws SQLException {
        this.showSql = showSql;
        String url = "jdbc:" + database.getDbType() + "://" + host + ":" + port + "/" + db;
        connection = DriverManager.getConnection(url, user, password);
    }

    public void save(Object object) throws SQLException {
        Map<String, String> objectMap = convertToMapThis(object);

        String sql = getSqlInsertQuery(object.getClass().getSimpleName(), objectMap);
        PreparedStatement ps = connection.prepareStatement(log(sql));
        executeWithParams(ps, objectMap);
    }

    private String getSqlInsertQuery(String tableName, Map<String, String> objectMap) {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into ").append(tableName);

        List<String> fields = new ArrayList<>(objectMap.keySet());

        appendCommaSeparated(builder, fields);
        builder.append("values");
        List<String> parameterQuestionMarks = new ArrayList<>();
        for (String field : fields) {
            parameterQuestionMarks.add("?");
        }

        appendCommaSeparated(builder, parameterQuestionMarks);
        return builder.toString();
    }

    private void executeWithParams(PreparedStatement ps, Map<String, String> objectMap) throws SQLException {
        List<String> fields = new ArrayList<>(objectMap.keySet());
        for (int i = 0; i < fields.size(); i++) {
            ps.setString(i + 1, objectMap.get(fields.get(i)));
        }

        ps.execute();
    }

    private void appendCommaSeparated(StringBuilder builder, Collection<String> values) {
        builder.append("(");
        for (String value : values) {
            builder.append(value);
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(")");
    }

    public void delete(Object object) throws SQLException {
        StringBuilder builder = new StringBuilder();
        builder
                .append("delete from ")
                .append(object.getClass().getSimpleName())
                .append(" ")
                .append("where");

        Map<String, String> objectMap = convertToMapThis(object);
        ArrayList<String> fields = new ArrayList<>(objectMap.keySet());
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            builder.append(" ").append(field).append("=").append("?");
            if (i != fields.size() - 1) {
                builder.append(" and");
            }
        }

        String sql = builder.toString();
        PreparedStatement ps = connection.prepareStatement(log(sql));
        executeWithParams(ps, objectMap);
    }

    public <T> List<T> getAll(Class<T> clazz) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "select * from " + clazz.getSimpleName();
        ResultSet resultSet = statement.executeQuery(log(sql));

        List<T> resultList = new ArrayList<>();

        while (resultSet.next()) {
            try {
                Constructor<T> constructor = clazz.getConstructor();
                T t = constructor.newInstance();
                populateFields(t, resultSet);

                resultList.add(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return resultList;
    }

    private <T> void populateFields(T t, ResultSet resultSet) throws IllegalAccessException, SQLException {
        Field[] declaredFields = t.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            declaredField.set(t, resultSet.getString(declaredField.getName()));
        }
    }

    public void register(Class<?> clazz) throws SQLException {
        String name = clazz.getSimpleName();
        StringBuilder builder = new StringBuilder();
        builder.append("create table if not exists ").append(name);
        builder.append("(");
        Map<String, String> fieldDescriptors = getFieldDescriptors(clazz);
        for (Map.Entry<String, String> fieldDescriptor : fieldDescriptors.entrySet()) {
            builder
                    .append(fieldDescriptor.getKey())
                    .append(" ")
                    .append(fieldDescriptor.getValue())
                    .append(",");
        }

        builder.deleteCharAt(builder.length() - 1);
        builder.append(")");

        String sql = builder.toString();

        Statement statement = connection.createStatement();

        statement.executeUpdate(log(sql));
    }

    private Map<String, String> getFieldDescriptors(Class<?> clazz) {
        Map<String, String> fieldDescriptors = new HashMap<>();
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            String name = declaredField.getName();
            String fieldType = dbRepresentationOf(declaredField.getType());
            fieldDescriptors.put(name, fieldType);
        }

        return fieldDescriptors;
    }

    private Map<String, String> convertToMapThis(Object obj) {
        Map<String, String> map = new HashMap<>();

        try {
            Field[] declaredFields = obj.getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                Object value = declaredField.get(obj);
                map.put(declaredField.getName(), value.toString());
            }

            return map;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String dbRepresentationOf(Class<?> clazz) {
        if (String.class.equals(clazz)) {
            return "text";
        }

        throw new IllegalArgumentException();
    }

    private String log(String sql) {
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";

        if (showSql) {
            System.out.println(ANSI_RED + sql + ANSI_RESET);
        }
        return sql;
    }

}
