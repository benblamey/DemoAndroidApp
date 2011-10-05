It is not necessary to provide ldpi versions of all drawables.


"However, when the system is looking for a density-specific resource and does not find it in 
the density-specific directory, it won't always use the default resources. The system may 
instead use one of the other density-specific resources in order to provide better results 
when scaling. For example, when looking for a low-density resource and it is not available, 
the system prefers to scale-down the high-density version of the resource, because the system 
can easily scale a high-density resource down to low-density by a factor of 0.5, with fewer 
artifacts, compared to scaling a medium-density resource by a factor of 0.75."

http://developer.android.com/guide/practices/screens_support.html