package de.hgv.model

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "pictures")
class Picture(@Id val id: String, val time: Date, val type: String)