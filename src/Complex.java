public class Complex
{
    private double real, imaginary;
    private static Complex zero, one, I;

    public Complex(double re, double img)
    {
        real = re;
        imaginary = img;
    }

    public double getReal()
    {
        return real;
    }

    public double getImaginary()
    {
        return imaginary;
    }

    public String toString()
    {
        if (imaginary >= 0)
            return "("+real+" + "+imaginary+"i)";
        return "("+real+" - "+Math.abs(imaginary)+"i)";
    }

    public Complex plus(Complex other)
    {
        return new Complex(this.real+other.real, this.imaginary+other.imaginary);
    }

    public Complex times(Complex other)
    {
        return new Complex(this.real*other.real - this.imaginary*other.imaginary,
                           this.real*other.imaginary + this.imaginary*other.real);
    }

    public Complex times(double n)
    {
        return new Complex(n*this.real, n*this.imaginary);
    }

    public Complex squared()
    {
        return this.times(this);
    }

    public static Complex one()
    {
        if (one == null)
            one = new Complex(1,0);
        return one;
    }

    public static Complex zero()
    {
        if (zero == null)
            zero = new Complex(0,0);
        return zero;
    }

    public static Complex I()
    {
        if (I == null)
            I = new Complex(0,1);
        return I;
    }

    public double magnitude_squared()
    {
        return real*real+imaginary*imaginary;
    }

    public double magnitude()
    {
        return Math.sqrt(magnitude_squared());
    }
}
