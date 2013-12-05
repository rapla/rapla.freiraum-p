package org.rapla.plugin.freiraum.common;

import java.util.List;

public class Event
{
	String name;
	String start;
	String end;
	List<ResourceDescriptor> resources;
	
	public Event()
	{
	}

	public Event(String name,String begin, String end, List<ResourceDescriptor> resources) {
		super();
		this.name = name;
		this.start = begin;
		this.end = end;
		this.resources = resources;
	}

	public String getBegin() {
		return start;
	}

	public String getEnd() {
		return end;
	}

	public List<ResourceDescriptor> getResources() {
		return resources;
	}
	
	public String toString() 
	{
		return name + " " +start + "-" + end ;
	}
	
	
}