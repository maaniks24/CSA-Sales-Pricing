package com.levantare.pricing.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "CsaSoftware")
@Table(name = "software")
@Getter
@Setter
public class Software {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 30)
    private String code;
    @NotBlank @Size(max = 200)
    private String name;
    @Size(max = 1000)
    private String description;
    /** Text shown in the "Scope" column of the cost table. {assets} is replaced by the covered asset count. */
    @Size(max = 2000)
    private String scopeText;
    /** Subscription price for one block of {@link #assetBlock} assets. Larger asset counts scale proportionately. */
    @NotNull @DecimalMin("0")
    private BigDecimal price;
    @Min(1)
    private int assetBlock = 1;
    @NotNull @DecimalMin("0")
    private BigDecimal implementationCharge = BigDecimal.ZERO;
    @Size(max = 2000)
    private String implementationText;
    private boolean active = true;
}
