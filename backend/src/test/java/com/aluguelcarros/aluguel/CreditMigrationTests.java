package com.aluguelcarros.aluguel;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CreditMigrationTests {
    @Test void migratesExistingCreditWithoutRewritingAcceptedTerms() throws Exception {
        String url="jdbc:h2:mem:legacy_credit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR";
        try(var anchor=DriverManager.getConnection(url,"sa","")) {
        var source=new org.springframework.jdbc.datasource.SingleConnectionDataSource(anchor,true);
        Flyway.configure().dataSource(source).defaultSchema("locadora")
            .locations("classpath:db/migration").target("1").load().migrate();
        try(var s=anchor.createStatement()) {
            s.execute("set schema locadora");
            s.execute("insert into accounts(id,username,password_hash,name,role) values (1,'client','hash','Nome atual','CLIENTE'),(2,'company','hash','Empresa','EMPRESA'),(3,'bank','hash','Banco atual','BANCO')");
            s.execute("insert into vehicles(id,registration,year,brand,model,plate,daily_rate,owner_id,company_id) values(1,'001',2025,'Fiat','Argo','ABC1D23',100,2,2)");
            s.execute("insert into rentals(id,client_id,vehicle_id,company_id,bank_id,start_date,end_date,total,credit_amount,installments,status,created_at) values(1,1,1,2,3,'2025-01-01','2025-01-04',300,200,2,'CONTRATADO',current_timestamp)");
            s.execute("insert into contracts(id,rental_id,accepted_at,terms) values(1,1,current_timestamp,'Texto aceito pelo Nome antigo; Banco antigo; credito 200 em 2 parcelas')");
        }
        var flyway=Flyway.configure().dataSource(source).defaultSchema("locadora").locations("classpath:db/migration").load();
        flyway.migrate(); flyway.migrate();
        try(var s=anchor.createStatement();var r=s.executeQuery("select contract_id,amount,installments,terms from locadora.credit_contracts")) {
            assertThat(r.next()).isTrue(); assertThat(r.getLong(1)).isEqualTo(1);
            assertThat(r.getBigDecimal(2)).isEqualByComparingTo("200"); assertThat(r.getInt(3)).isEqualTo(2);
            assertThat(r.getString(4)).contains("Nome antigo","Banco antigo").doesNotContain("Nome atual","Banco atual");
            assertThat(r.next()).isFalse();
        }
        }
    }
}
