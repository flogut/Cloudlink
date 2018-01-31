package de.hgv.model

import javax.persistence.*

@Entity
@Table(name = "user")
class User(@Id val username: String, val password: String, @get:Enumerated(EnumType.ORDINAL) val role: MyRole)