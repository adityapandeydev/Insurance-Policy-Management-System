import { ReactNode } from 'react';

export const Table = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <div className={`w-full overflow-x-auto ${className}`}>
    <table className="w-full text-left text-sm whitespace-nowrap">
      {children}
    </table>
  </div>
);

export const TableHeader = ({ children }: { children: ReactNode }) => (
  <thead className="bg-foreground/5 text-foreground/70 uppercase text-xs font-semibold border-y border-border">
    {children}
  </thead>
);

export const TableRow = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <tr className={`border-b border-border/50 hover:bg-foreground/[0.02] transition-colors ${className}`}>
    {children}
  </tr>
);

export const TableHead = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <th className={`px-6 py-3 ${className}`}>
    {children}
  </th>
);

export const TableCell = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <td className={`px-6 py-4 text-foreground ${className}`}>
    {children}
  </td>
);
