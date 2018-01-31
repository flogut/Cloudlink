package de.hgv.model

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "data")
class Data(@Id val id: String, @get:Enumerated(EnumType.ORDINAL) val type: DataType, val value: Double, val time: Date)