package de.hdm.seCode.example.world;

import de.hdm.seCode.example.world.bank.security.SKonto;

public interface IPerson {
	public String getName();
	public Person setName(String name) ;
	public SPerson getPartner() ;
	public Person setPartner(SPerson partner) ;
	public SKonto getKonto() ;
	public Person setKonto(SKonto konto) ;
	public Person addFreund(SPerson person);
	public void doSex();
	public void go();
}
