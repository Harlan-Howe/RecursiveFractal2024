import java.util.Objects;

public class ComplexRange
{
    private Complex min, max;

    public ComplexRange(Complex min, Complex max)
    {
        this.min = min;
        this.max = max;
    }

    public String toString()
    {
        return min.toString() + " --> "+max.toString();
    }

    public Complex getMin() {return min;}
    public Complex getMax() {return max;}

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplexRange that = (ComplexRange) o;
        return min.equals(that.min) && max.equals(that.max);
    }

}
