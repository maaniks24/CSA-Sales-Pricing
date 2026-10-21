package com.levantare.pricing.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Service master: one costed phase (FAR, PV, DC, DM, DR, TG ...). */
@Entity(name = "CsaServiceItem")
@Table(name = "service_master")
@Getter
@Setter
public class ServiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 30)
    private String code;
    @NotBlank @Size(max = 200)
    private String name;
    /** Wording used in the proposal timeline table. */
    @Size(max = 500)
    private String activity;
    private int sequenceNo;
    /** PPD-PP: assets handled per person per day. */
    @NotNull @DecimalMin(value = "0", inclusive = false)
    private BigDecimal assetsPerDay;
    @NotNull @Enumerated(EnumType.STRING)
    private ResourceType resourceType = ResourceType.TRACKING;
    private boolean includeRoom;
    private boolean includeFood;
    private boolean defaultInScope = true;
    private boolean active = true;
}
