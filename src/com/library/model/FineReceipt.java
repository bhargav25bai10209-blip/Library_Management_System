package com.library.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a payment transaction receipt for settled library fines.
 */
public class FineReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    private String receiptId;
    private String transactionId;
    private String memberId;
    private String memberName;
    private double amountPaid;
    private String paymentMethod;
    private LocalDateTime paymentDate;

    public FineReceipt() {
        this.paymentDate = LocalDateTime.now();
    }

    public FineReceipt(String receiptId, String transactionId, String memberId,
                       String memberName, double amountPaid, String paymentMethod) {
        this.receiptId = receiptId;
        this.transactionId = transactionId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.paymentDate = LocalDateTime.now();
    }

    public String getReceiptId() { return receiptId; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
