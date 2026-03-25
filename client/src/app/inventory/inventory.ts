export interface Inventory {
  _id?: string, // Added for delete functionality
  item: string,
  description: string,
  brand: string,
  color:string,
  size: string,
  type: string,
  material:string,
  count: number,
  quantity: number,
  notes: string
}
