package com.levantare.pricing.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "CsaCustomer")
@Table(name = "customer")
@Getter
@Setter
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 30)
    private String code;
    @NotBlank @Size(max = 200)
    private String name;
    @Size(max = 1000)
    private String address;
    private String city;
    private String vertical;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    @Min(0)
    private int assetCount;
    @Min(1)
    private int branchCount = 1;
    /** Customer specific discount, applied by default on new proposals. */
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private boolean active = true;
}
