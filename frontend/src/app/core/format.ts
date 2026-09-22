/** Current local time as the value format of <input type="datetime-local">. */
export function nowForInput(offsetHours = 0): string {
  const d = new Date(Date.now() + offsetHours * 3_600_000);
  d.setSeconds(0, 0);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
