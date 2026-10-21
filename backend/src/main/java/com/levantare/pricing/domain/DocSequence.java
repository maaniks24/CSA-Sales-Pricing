package com.levantare.pricing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "CsaDocSequence")
@Table(name = "doc_sequence")
@Getter
@Setter
public class DocSequence {
    @Id
    private String seqKey;
    private long lastValue;
}
