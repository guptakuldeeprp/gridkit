package org.gridkit.coherence.profile;




/**
 * @author Alexey Ragozin (aragozin@griddynamics.net)
 */
public class Histogram implements Sampler, StatValue, Cloneable {

    long lowLimit;
	long step;
	int size;

	long scale;

	long[] totalCountBuckets;
	double[] totalSumBuckets;

	transient double count;
	transient double total;
	transient double avg;
	transient double stdDev;

	protected Histogram() {
	    // for serializer
	}
	
	public Histogram(long scale, long min, long max, int size)
	{
		this.scale = scale;
		this.lowLimit = min * scale;
		this.step = ((max - min) * scale) / size ;
		this.size = size;
		this.totalCountBuckets = new long[size + 2];
		this.totalSumBuckets = new double[size + 2];
	}
	
	@Override
	public Histogram clone() {
        try {
            Histogram that = (Histogram) super.clone();
            that.totalCountBuckets = that.totalCountBuckets.clone();
            that.totalSumBuckets = that.totalSumBuckets.clone();            
            return that;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
	    for(int i = 0; i != totalCountBuckets.length; ++i) {
	        totalCountBuckets[i] = 0;
	        totalSumBuckets[i] = 0;
	    }
	}
	
	public Histogram invert() {
	    Histogram hist = new Histogram();
	    hist.scale = scale;
	    hist.lowLimit = lowLimit;
	    hist.step = step;
	    hist.size = size;
	    hist.totalCountBuckets = totalCountBuckets.clone();
	    hist.totalSumBuckets = totalSumBuckets.clone();
	    for(int i = 0; i != totalCountBuckets.length; ++i) {
	        hist.totalCountBuckets[i] = -totalCountBuckets[i]; 
	        hist.totalSumBuckets[i] = -totalSumBuckets[i]; 
	    }
	    return hist;
	}

	public void addSample(long value)
	{
		int idx;
		if (value < lowLimit)
		{
			idx = 0;
		}
		else
		{
			idx = (int) ((value - lowLimit) / step);
			if (idx > size)
			{
				idx = size;
			}
			++idx;
		}

		try {
    		++totalCountBuckets[idx];
    		totalSumBuckets[idx] += (((double)value) / scale);
		}
		catch(ArrayIndexOutOfBoundsException e) {
		    new String();
		}
	}

	public void addHistogram(Histogram histogram) {
	    check("lowLimit", lowLimit, histogram.lowLimit);
	    check("step", step, histogram.step);
	    check("size", size, histogram.size);
	    check("scale", scale, histogram.scale);
	    
	    for(int i = 0; i != totalCountBuckets.length; ++i) {
	        totalCountBuckets[i] += histogram.totalCountBuckets[i];
	    }
	    for(int i = 0; i != totalSumBuckets.length; ++i) {
	        totalSumBuckets[i] += histogram.totalSumBuckets[i];
	    }
	}
	
	private void check(String field, Object o1, Object o2) {
	    if (!((o1 == null && o2 == null) || (o1.equals(o2)))) {
	        throw new IllegalArgumentException("Incomaptible histograms, field '" + field +"' " + o1 + " != " + o2);
	    }
	}
	
	public double getCount() {
		return count;
	}

	public double getAvg() {
		return avg;
	}

	public double getTotal() {
		return total;
	}

	public double getStdDev() {
		return stdDev;
	}

	public void updateStats()
	{
		double dCount = 0;
		double dTotal = 0;

        for(int i = 0; i != totalCountBuckets.length; ++i)
        {
            dCount += totalCountBuckets[i];
            dTotal += totalSumBuckets[i];
        }
		
		this.count = dCount;
		this.total = dTotal;
		this.avg = dTotal / dCount;

		double dTotalVar = 0;
		for(int i = 0; i != totalCountBuckets.length; ++i)
		{
			if (totalCountBuckets[i] > 0)
			{
				double var = totalSumBuckets[i] / totalCountBuckets[i];
				var -= avg;

				var = var * var;

				dTotalVar += var * totalCountBuckets[i];
			}
		}

		this.stdDev = Math.sqrt(dTotalVar / dCount);
	}
}
