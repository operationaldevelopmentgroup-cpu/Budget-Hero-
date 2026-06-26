package com.example.budgethero.ui.dashboard

import com.example.budgethero.data.model.Bill
import com.example.budgethero.data.model.BillFrequency
import com.example.budgethero.data.model.Income
import com.example.budgethero.data.model.LineItem
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import com.opencsv.CSVWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ExportUtils {

    data class ExportData(
        val bills: List<Bill>,
        val income: List<Income>,
        val lineItems: List<LineItem>,
        val dailyGoal: Double,
        val totalMonthlyBilling: Double,
        val daysInMonth: Int,
        val referenceDate: LocalDate
    )

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val currencyFormatter = "%.2f"

    private fun calculateMonthlyAmount(bill: Bill, daysInMonth: Int): Double {
        return when (bill.frequency) {
            BillFrequency.DAILY -> bill.amount * daysInMonth
            BillFrequency.WEEKLY -> bill.amount * (daysInMonth / 7.0)
            BillFrequency.MONTHLY -> bill.amount
            BillFrequency.YEARLY -> bill.amount / 12.0
        }
    }

    fun generatePdf(outputStream: OutputStream, data: ExportData) {
        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        document.add(Paragraph("BudgetHero - Activity Export").setFontSize(20f))
        document.add(Paragraph("Generated on: ${java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}"))

        // Billing Summary
        document.add(Paragraph("\nMonthly Billing Summary").setFontSize(16f))
        val summaryTable = Table(UnitValue.createPointArray(floatArrayOf(300f, 200f)))
        summaryTable.addCell("Days in Month")
        summaryTable.addCell(data.daysInMonth.toString())
        summaryTable.addCell("Total Estimated Monthly Billing")
        summaryTable.addCell(String.format(Locale.US, currencyFormatter, data.totalMonthlyBilling))
        summaryTable.addCell("Daily Work-Day Goal")
        summaryTable.addCell(String.format(Locale.US, currencyFormatter, data.dailyGoal))
        document.add(summaryTable)

        // Income Section
        document.add(Paragraph("\nIncome Activity").setFontSize(16f))
        val incomeTable = Table(UnitValue.createPointArray(floatArrayOf(100f, 300f, 100f)))
        incomeTable.addHeaderCell("Date")
        incomeTable.addHeaderCell("Description")
        incomeTable.addHeaderCell("Amount")
        
        data.income.sortedByDescending { it.date }.forEach {
            incomeTable.addCell(it.date.format(dateFormatter))
            incomeTable.addCell(it.description)
            incomeTable.addCell(String.format(Locale.US, currencyFormatter, it.amount))
        }
        document.add(incomeTable)
        document.add(Paragraph("Total Income: ${String.format(Locale.US, currencyFormatter, data.income.sumOf { it.amount })}"))

        // Line Items Section
        document.add(Paragraph("\nSpending Activity").setFontSize(16f))
        val spendingTable = Table(UnitValue.createPointArray(floatArrayOf(100f, 300f, 100f, 100f)))
        spendingTable.addHeaderCell("Date")
        spendingTable.addHeaderCell("Description")
        spendingTable.addHeaderCell("Amount")
        spendingTable.addHeaderCell("Budget?")

        data.lineItems.sortedByDescending { it.date }.forEach {
            spendingTable.addCell(it.date.format(dateFormatter))
            spendingTable.addCell(it.description)
            spendingTable.addCell(String.format(Locale.US, currencyFormatter, it.amount))
            spendingTable.addCell(if (it.isBudgetItem) "Yes" else "No")
        }
        document.add(spendingTable)
        document.add(Paragraph("Total Spending: ${String.format(Locale.US, currencyFormatter, data.lineItems.sumOf { it.amount })}"))

        // Bills Section
        document.add(Paragraph("\nActive Bills & Monthly Projections").setFontSize(16f))
        val billsTable = Table(UnitValue.createPointArray(floatArrayOf(150f, 100f, 100f, 150f)))
        billsTable.addHeaderCell("Name")
        billsTable.addHeaderCell("Base Amount")
        billsTable.addHeaderCell("Frequency")
        billsTable.addHeaderCell("Monthly Cost")

        data.bills.forEach {
            val monthlyCost = calculateMonthlyAmount(it, data.daysInMonth)
            billsTable.addCell(it.name)
            billsTable.addCell(String.format(Locale.US, currencyFormatter, it.amount))
            billsTable.addCell(it.frequency.name)
            billsTable.addCell(String.format(Locale.US, currencyFormatter, monthlyCost))
        }
        document.add(billsTable)
        document.add(Paragraph("Total Monthly Billing: ${String.format(Locale.US, currencyFormatter, data.totalMonthlyBilling)}"))

        document.close()
    }

    fun generateCsv(outputStream: OutputStream, data: ExportData) {
        val writer = CSVWriter(OutputStreamWriter(outputStream))

        // Summary
        writer.writeNext(arrayOf("SECTION: SUMMARY"))
        writer.writeNext(arrayOf("Days in Month", data.daysInMonth.toString()))
        writer.writeNext(arrayOf("Total Monthly Billing", String.format(Locale.US, currencyFormatter, data.totalMonthlyBilling)))
        writer.writeNext(arrayOf("Daily Work-Day Goal", String.format(Locale.US, currencyFormatter, data.dailyGoal)))
        writer.writeNext(arrayOf(""))

        // Income
        writer.writeNext(arrayOf("SECTION: INCOME"))
        writer.writeNext(arrayOf("Date", "Description", "Amount"))
        data.income.sortedByDescending { it.date }.forEach {
            writer.writeNext(arrayOf(
                it.date.format(dateFormatter),
                it.description,
                String.format(Locale.US, currencyFormatter, it.amount)
            ))
        }
        writer.writeNext(arrayOf("Total Income", "", String.format(Locale.US, currencyFormatter, data.income.sumOf { it.amount })))
        writer.writeNext(arrayOf(""))

        // Spending
        writer.writeNext(arrayOf("SECTION: SPENDING"))
        writer.writeNext(arrayOf("Date", "Description", "Amount", "Is Budget Item"))
        data.lineItems.sortedByDescending { it.date }.forEach {
            writer.writeNext(arrayOf(
                it.date.format(dateFormatter),
                it.description,
                String.format(Locale.US, currencyFormatter, it.amount),
                if (it.isBudgetItem) "TRUE" else "FALSE"
            ))
        }
        writer.writeNext(arrayOf("Total Spending", "", String.format(Locale.US, currencyFormatter, data.lineItems.sumOf { it.amount })))
        writer.writeNext(arrayOf(""))

        // Bills
        writer.writeNext(arrayOf("SECTION: BILLS"))
        writer.writeNext(arrayOf("Name", "Base Amount", "Frequency", "Monthly Cost"))
        data.bills.forEach {
            val monthlyCost = calculateMonthlyAmount(it, data.daysInMonth)
            writer.writeNext(arrayOf(
                it.name,
                String.format(Locale.US, currencyFormatter, it.amount),
                it.frequency.name,
                String.format(Locale.US, currencyFormatter, monthlyCost)
            ))
        }
        writer.writeNext(arrayOf("Total Monthly Billing", "", "", String.format(Locale.US, currencyFormatter, data.totalMonthlyBilling)))

        writer.close()
    }
}
