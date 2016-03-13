import groovy.sql.Sql

import java.sql.Connection
import java.sql.Driver
import com.mysql.jdbc.Driver


/**
 * Created by roland on 20.02.16.
 */

class Test {



    public static void main(String[] args) {


        def driver
        def databaseClassName // classname for JDBC driver
        def props //database properties
        def Connection conn //database connection
        //properties in order to connect to the database
        props = new Properties()
        props.setProperty("user", "roland")
        props.setProperty("password", "01eins10")


            databaseClassName  = "com.mysql.jdbc.Driver"
            driver = Class.forName(databaseClassName).newInstance() as Driver
            conn = driver.connect("jdbc:mysql://127.0.0.1:3306/sakila",props)

            def sql = new Sql(conn)



        sql.eachRow('select * from FILM') { row ->
            println "$row.film_id $row.description"
        }
    }
}