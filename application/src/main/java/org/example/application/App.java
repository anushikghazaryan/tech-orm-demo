package org.example.application;

import org.example.techorm.Database;
import org.example.techorm.TechOrm;

import java.sql.SQLException;

public class App {

    public static void main(String[] args) throws SQLException {
        TechOrm orm = new TechOrm(
                Database.POSTGRES, true,
                "localhost", 5432, "test",
                "test", "test"
        );

        // Create table if not exists
        orm.register(org.example.application.Student.class);

        org.example.application.Student student = new org.example.application.Student();
        student.setFirstName("John");
        student.setLastName("Smith");

        orm.save(student);
        System.out.println(orm.getAll(org.example.application.Student.class));
        orm.save(student);
        System.out.println(orm.getAll(org.example.application.Student.class));
        orm.delete(student);
        System.out.println(orm.getAll(org.example.application.Student.class));
    }

}
