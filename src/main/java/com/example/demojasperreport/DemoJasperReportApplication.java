package com.example.demojasperreport;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

@Log4j2
@SpringBootApplication
public class DemoJasperReportApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoJasperReportApplication.class, args);
	}

	@Autowired
	private DataSource dataSource;
	@Autowired
	private EmployeeRepository  employeeRepository;
	@Autowired
	private EmailRepository emailRepository;

	@Bean
	public ApplicationRunner createJasperReportRunner() {
		return args -> {
			log.info("Starting JasperReportRunner");
			log.info("Create Dummy Data");
			creatDummyData();

			log.info("Compiling employeeReport.jrxml");
			InputStream employeeReportStream = getClass().getClassLoader().getResourceAsStream("reports/employeeReport.jrxml");
			JasperReport jasperReport = JasperCompileManager.compileReport(employeeReportStream);
			JRSaver.saveObject(jasperReport, "employeeReport.jasper");
			
			log.info("Compiling employeeEmailReport.jrxml");
			InputStream emailReportStream = getClass().getClassLoader().getResourceAsStream("reports/employeeEmailReport.jrxml");
			JRSaver.saveObject(JasperCompileManager.compileReport(emailReportStream), "employeeEmailReport.jasper");

			log.info("Proceeding employeeReport");
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("title", "Employee Report");
			parameters.put("minSalary", 150000.0);
			parameters.put("condition", " LAST_NAME ='Smith' ORDER BY FIRST_NAME");
			JasperPrint printOut = JasperFillManager.fillReport(jasperReport, parameters, dataSource.getConnection());
			JRPdfExporter pdfExporter = new JRPdfExporter();
			pdfExporter.setExporterInput(new SimpleExporterInput(printOut));
			pdfExporter.setExporterOutput(new SimpleOutputStreamExporterOutput("/tmp/out.pdf"));
			pdfExporter.exportReport();
			log.info("exported /tmp/out.pdf");

			JRDocxExporter docxExporter = new JRDocxExporter();
			docxExporter.setExporterInput(new SimpleExporterInput(printOut));
			docxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput("/tmp/out.docx"));
			docxExporter.exportReport();
			log.info("exported /tmp/out.docx");
			log.info("JasperReportRunner completed");
			
		};
	}

	private void creatDummyData() {
		Employee employee = new Employee.EmployeeBuilder()
			.firstName("John")
			.lastName("Smith")
			.salary(150000.)
			.build();

		employee = employeeRepository.save(employee);

		Email email = new Email.EmailBuilder()
			.address("john@smith.com")
			.employee(employee)
			.build();

		emailRepository.save(email);
	}

}

@Data
@Builder
@Entity
class Employee {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Column(name = "FIRST_NAME")
	private String firstName;
	@Column(name = "LAST_NAME")
	private String lastName;
	@Column(name = "SALARY")
	private Double salary;
}

@Data
@Builder
@Entity
class Email {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Column(name = "ADDRESS")
	private String address;
	@ManyToOne
	@JoinColumn(name = "id_employee")
	private Employee employee;
}

@Repository
interface EmployeeRepository extends JpaRepository<Employee, Long> { }

@Repository
interface EmailRepository extends JpaRepository<Email, Long> { }