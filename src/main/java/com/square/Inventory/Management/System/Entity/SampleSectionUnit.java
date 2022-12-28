package com.square.Inventory.Management.System.Entity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

public class SampleSectionUnit {
    @Id
    @Column(name = "ssuID")
    private Integer ssuID;

    @Column(name="ssuName")
    private String ssuName;

    @Column(name="location")
    private String location;

    @Column(name="numberOfEmployee")
    private Integer numberOfEmployee;
    @OneToOne
    @JoinColumn(name = "userId")
    private User user;
}