package com.alouder.bibles.data;

public interface INavigatable<T>
{
	public abstract boolean hasPrevious();
	
	public abstract boolean hasNext();
	
	public abstract T getPrevious();
	
	public abstract T getNext();
	
	public abstract String getTitle();
	
	public abstract String getAbbreviation();
}
